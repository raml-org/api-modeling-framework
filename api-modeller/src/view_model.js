"use strict";
const ko = require("knockout");
const load_modal_1 = require("./view_models/load_modal");
const electron_1 = require("electron");
const nav_1 = require("./view_models/nav");
var createModel = monaco.editor.createModel;
class ViewModel {
    constructor(editor) {
        this.editor = editor;
        this.navigatorSection = ko.observable("files");
        this.editorSection = ko.observable("raml");
        this.references = ko.observableArray([]);
        this.selectedReference = ko.observable(null);
        this.nav = new nav_1.Nav("document");
        this.loadModal = new load_modal_1.LoadModal();
        this.documentLevel = "document";
        this.model = undefined;
        // events we are subscribed
        this.loadModal.on(load_modal_1.LoadModal.LOAD_FILE_EVENT, (data) => {
            this.apiModellerWindow().parseModelFile(data.type, data.location, (err, model) => {
                if (err) {
                    console.log(err);
                    alert(err);
                }
                else {
                    this.model = model;
                    this.resetDocuments();
                }
            });
        });
        this.nav.on(nav_1.Nav.DOCUMENT_LEVEL_SELECTED_EVENT, (level) => {
            this.onDocumentLevelChange(level);
        });
        this.editorSection.subscribe((section) => this.onEditorSectionChange(section));
    }
    apiModellerWindow() { return electron_1.remote.getCurrentWindow(); }
    apply(location) {
        window["viewModel"] = this;
        ko.applyBindings(this);
    }
    onDocumentLevelChange(level) {
        console.log(`** New document level ${level}`);
        this.documentLevel = level;
        this.resetDocuments();
    }
    // Reset the view model state when a document has changed
    resetDocuments() {
        if (this.model != null) {
            // we reset the list of references for this model
            this.resetReferences();
            // We generate the RAML representation
            this.model.toRaml(this.documentLevel, (err, string) => {
                if (err != null) {
                    console.log("Error generating RAML");
                    console.log(err);
                }
                else {
                    if (this.editorSection() === "raml") {
                        this.editor.setModel(createModel(this.model.ramlString, "yaml"));
                    }
                }
            });
            // We generate the JSON representation
            this.model.toOpenAPI(this.documentLevel, (err, string) => {
                if (err != null) {
                    console.log("Error getting OpenAPI");
                    console.log(err);
                }
                else {
                    if (this.editorSection() === "open-api") {
                        this.editor.setModel(createModel(this.model.openAPIString, "json"));
                    }
                }
            });
        }
    }
    onEditorSectionChange(section) {
        if (section === "raml") {
            if (this.model != null) {
                this.editor.setModel(createModel(this.model.ramlString, "yaml"));
            }
            else {
                this.editor.setModel(createModel("# no model loaded", "yaml"));
            }
        }
        else {
            if (this.model != null) {
                this.editor.setModel(createModel(this.model.openAPIString, "json"));
            }
            else {
                this.editor.setModel(createModel("// no model loaded", "json"));
            }
        }
    }
    // Reset the list of references for the current model
    resetReferences() {
        console.log("Setting references");
        if (this.model != null) {
            const location = this.model.location();
            this.selectedReference(this.makeReference(location, location));
            if (this.documentLevel === "document") {
                this.references.removeAll();
                this.model.references().forEach(ref => this.references.push(this.makeReference(location, ref)));
            }
            else {
                this.references.removeAll();
                this.references.push(this.makeReference(location, this.model.location()));
            }
        }
        console.log("TOTAL REFERENCES " + this.references.length);
    }
    makeReference(currentLocation, reference) {
        console.log("*** Making reference " + reference);
        const parts = currentLocation.split("/");
        parts.pop();
        const currentLocationDir = parts.join("/") + "/";
        const isRemote = reference.startsWith("http");
        if (reference.startsWith(currentLocationDir)) {
            return {
                type: (isRemote ? "remote" : "local"),
                name: reference.replace(currentLocationDir, ""),
                value: reference
            };
        }
        else {
            const refParts = reference.split("/");
            let name;
            if (refParts.length > 3) {
                const n = refParts.pop();
                const n1 = refParts.pop();
                name = `.../${n1}/${n}`;
            }
            else {
                name = refParts.join("/");
            }
            return {
                type: (isRemote ? "remote" : "local"),
                name: name,
                value: reference
            };
        }
    }
}
exports.ViewModel = ViewModel;
//# sourceMappingURL=view_model.js.map