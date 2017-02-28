import * as ko from "knockout";
import { LoadModal, LoadFileEvent } from "./view_models/load_modal";
import { ModelProxy, ModelLevel } from "./main/model_proxy";
import { remote } from "electron";
import { ApiModellerWindow } from "./main/api_modeller_window";
import { Nav } from "./view_models/nav";
import IStandaloneCodeEditor = monaco.editor.IStandaloneCodeEditor;
import createModel = monaco.editor.createModel;
import { Document, Fragment, Module } from "./main/units_model";
import { label } from "./utils";
import { UI } from "./view_models/ui";
import { DomainElement, DomainModel } from "./main/domain_model";

export type NavigatorSection = "files" | "logic";
export type EditorSection = "raml" | "open-api" | "api-model";

export interface ReferenceFile {
    id: string;
    label: string;
    type: "local" | "remote"
}

export class ViewModel {

    public navigatorSection: KnockoutObservable<NavigatorSection> = ko.observable<NavigatorSection>("files");
    public editorSection: KnockoutObservable<EditorSection> = ko.observable<EditorSection>("raml");
    public references: KnockoutObservableArray<ReferenceFile> = ko.observableArray<ReferenceFile>([]);
    public selectedReference: KnockoutObservable<ReferenceFile | null> = ko.observable<ReferenceFile | null>(null);
    public documentUnits: KnockoutObservableArray<Document> = ko.observableArray<Document>([]);
    public fragmentUnits: KnockoutObservableArray<Fragment> = ko.observableArray<Fragment>([]);
    public moduleUnits: KnockoutObservableArray<Module> = ko.observableArray<Module>([]);
    public domainUnits: KnockoutObservable<{ [kind: string]: DomainElement[] }> = ko.observable<{ [kind: string]: DomainElement[] }>({});
    public ui: UI = new UI();
    public nav: Nav = new Nav("document");
    public loadModal: LoadModal = new LoadModal();
    public documentLevel: ModelLevel = "document";
    public documentModel?: ModelProxy = undefined;
    public model?: ModelProxy = undefined;
    public generationOptions: KnockoutObservable<any> = ko.observable<any>({ "source-maps?": false });
    public generateSourceMaps: KnockoutObservable<string> = ko.observable<string>("no");
    public referenceToDomainUnits: { [id: string]: DomainModel[] } = {};


    constructor(public editor: IStandaloneCodeEditor) {
        // events we are subscribed
        this.loadModal.on(LoadModal.LOAD_FILE_EVENT, (data: LoadFileEvent) => {
            this.apiModellerWindow().parseModelFile(data.type, data.location, (err, model) => {
                if (err) {
                    console.log(err);
                    alert(err);
                } else {
                    this.documentModel = model;
                    this.model = model;
                    this.selectedReference(this.makeReference(this.documentModel!.location(), this.documentModel!.location()));
                    this.resetUnits();
                    this.resetReferences();
                    this.resetDocuments();
                }
            });
        });
        this.nav.on(Nav.DOCUMENT_LEVEL_SELECTED_EVENT, (level: ModelLevel) => {
            this.onDocumentLevelChange(level);
        });
        this.generateSourceMaps.subscribe((generate) => {
            if (generate === "yes") {
                this.generationOptions()["source-maps?"] = true;
            } else {
                this.generationOptions()["source-maps?"] = false;
            }
            this.resetDocuments();
        });
        this.editorSection.subscribe((section) => this.onEditorSectionChange(section));
    }

    public selectNavigatorFile(reference: ReferenceFile) {
        this.selectedReference(reference);
        if (this.documentModel != null) {
            if (this.documentModel.location() !== reference.id) {
                this.model = this.documentModel.nestedModel(reference.id);
            } else {
                this.model = this.documentModel;
            }
            this.resetDocuments()
        }
        this.resetDomainUnits();
    }

    public expandDomainUnit(unit: DomainElement) {
        unit["expanded"] = !unit["expanded"];
        this.domainUnits({});
        this.resetDomainUnits();
    }

    private onDocumentLevelChange(level: ModelLevel) {
        console.log(`** New document level ${level}`);
        this.documentLevel = level;
        if (level === "domain" && this.documentModel) {
            this.model = this.documentModel;
            this.selectedReference(this.makeReference(this.documentModel.location(), this.documentModel.location()));
        }
        this.resetDocuments();
        this.resetReferences();
        this.resetUnits();
    }



    protected apiModellerWindow(): ApiModellerWindow { return remote.getCurrentWindow() as ApiModellerWindow; }

    apply(location: Node) {
        window["viewModel"] = this;
        ko.applyBindings(this);
    }


    // Reset the view model state when a document has changed
    private resetDocuments() {
        if (this.model != null) {
            // We generate the RAML representation
            this.model.toRaml(this.documentLevel, this.generationOptions(), (err, string) => {
                if (err != null) {
                    console.log("Error generating RAML");
                    console.log(err);
                } else {
                    if (this.editorSection() === "raml") {
                        this.editor.setModel(createModel(this.model!.ramlString, "yaml"));
                    }
                }
            });

            // We generate the OpenAPI representation
            this.model.toOpenAPI(this.documentLevel, this.generationOptions(), (err, string) => {
                if (err != null) {
                    console.log("Error getting OpenAPI");
                    console.log(err);
                } else {
                    if (this.editorSection() === "open-api") {
                        this.editor.setModel(createModel(this.model!.openAPIString, "json"));
                    }
                }
            });

            // We generate the APIModel representation
            this.model.toAPIModel(this.documentLevel, this.generationOptions(), (err, string) => {
                if (err != null) {
                    console.log("Error getting ApiModel");
                    console.log(err);
                } else {
                    if (this.editorSection() === "api-model") {
                        this.editor.setModel(createModel(this.model!.apiModeltring, "json"));
                    }
                }
            });
        }
    }

    public resetDomainUnits() {
        const ref = this.selectedReference();
        const units = {};
        if (ref != null) {
            const domains = this.referenceToDomainUnits[ref.id] || [];
            domains.forEach(domain => {
                if (domain.root != null) {
                    const acc = units[domain.root.kind] || [];
                    acc.push(domain.root);
                    units[domain.root.kind] = acc;
                }
            });

        }
        this.domainUnits(units);
    }

    private onEditorSectionChange(section: EditorSection) {
        // Warning, models here mean MONACO EDITOR MODELS, don't get confused with API Models
        if (section === "raml") {
            if (this.model != null) {
                this.editor.setModel(createModel(this.model.ramlString, "yaml"));
            } else {
                this.editor.setModel(createModel("# no model loaded", "yaml"));
            }
        } else if (section == "open-api") {
            if (this.model != null) {
                this.editor.setModel(createModel(this.model!.openAPIString, "json"));
            } else {
                this.editor.setModel(createModel("// no model loaded", "json"));
            }
        } else {
            if (this.model != null) {
                this.editor.setModel(createModel(this.model!.apiModeltring, "json"));
            } else {
                this.editor.setModel(createModel("// no model loaded", "json"));
            }
        }
    }

    // Reset the list of references for the current model
    private resetReferences() {
        console.log("** Setting references");
        if (this.model != null && this.documentModel != null) {
            const location = this.model.location();
            if (this.documentLevel === "document") {
                this.references.removeAll();
                this.documentModel.references().forEach(ref => this.references.push(this.makeReference(location, ref)));
            } else {
                const documentModelReference = this.makeReference(location, location);
                this.references.removeAll();
                this.references.push(documentModelReference);
            }
        }
    }

    private makeReference(currentLocation: string, reference: string): ReferenceFile {
        console.log("*** Making reference " + reference);
        const parts = currentLocation.split("/");
        parts.pop();
        const currentLocationDir = parts.join("/") + "/";
        const isRemote = reference.startsWith("http");
        if (reference.startsWith(currentLocationDir)) {
            return {
                type: (isRemote ? "remote" : "local"),
                id: reference,
                label: label(reference)
            }
        } else {
            return {
                type: (isRemote ? "remote" : "local"),
                id: reference,
                label: label(reference),
            }
        }
    }

    private resetUnits() {
        if (this.documentModel != null) {
            this.documentModel.units(this.documentLevel, (err, units) => {
                if (err == null) {
                    console.log("Got the new units");
                    // reseting data
                    this.referenceToDomainUnits = {};
                    this.documentUnits.removeAll();
                    // Indexing document and domain units
                    units.documents.forEach(doc => {
                        this.indexDomainUnits(doc);
                        this.documentUnits.push(doc)
                    });
                    this.fragmentUnits.removeAll();
                    units.fragments.forEach(fragment => {
                        this.indexDomainUnits(fragment);
                        this.fragmentUnits.push(fragment)
                    });
                    this.moduleUnits.removeAll();
                    units.modules.forEach(module => {
                        this.indexDomainUnits(module);
                        this.moduleUnits.push(module)
                    });
                } else {
                    console.log("Error loading units");
                    console.log(err);
                }
            });
        } else {
            this.documentUnits.removeAll();
            this.fragmentUnits.removeAll();
            this.moduleUnits.removeAll();
        }
    }

    private indexDomainUnits(elm: Document | Fragment | Module) {
        console.log("INDEXING DOMAIN UNITS FOR " + elm.kind);
        const units: DomainModel[] = [];
        const reference = elm.id;
        this.referenceToDomainUnits[reference] = units;

        if (elm.kind === "Document") {
            const document = (elm as Document);
            if (document && document.encodes) {
                units.push(document.encodes.domain)
            }
            document.declares.forEach(dec => {
                units.push(dec.domain);
            })
        } else if (elm.kind === "Fragment") {
            const document = (elm as Fragment);
            if (document && document.encodes) {
                units.push(document.encodes.domain);
            }
        } else if (elm.kind === "Module") {
            const document = (elm as Module);
            document.declares.forEach(dec => {
                units.push(dec.domain);
            })
        }

        if (this.selectedReference() != null && this.selectedReference() !.id === reference) {
            console.log("Adding default domain units " + reference);
            this.resetDomainUnits();
        }
    }
}
