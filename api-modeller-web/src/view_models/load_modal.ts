import * as ko from "knockout";
import {ApiModellerWindow} from "../main/api_modeller_window";

export type ParserType = "raml" | "open-api";
export class LoadFileEvent {
    constructor(public type: ParserType, public location: string) {}
}

const baseUrl = window.location.protocol + "//" + window.location.host;

export class LoadModal {
    static LOAD_FILE_EVENT: string = "load-file";
    public fileUrl: KnockoutObservable<string> = ko.observable<string>("");
    public parserTypes: KnockoutObservableArray<any> = ko.observableArray([
        {name: "RAML 1.0", key: "raml"},
        {name: "OpenAPI 2.0", key: "open-api"}
    ]);
    public apiExamples: KnockoutObservableArray<any> = ko.observableArray([
        {name: "World Music API (RAML)", key: "raml", url: baseUrl + "/raml/world-music-api/api.raml"},
        {name: "Pet Store API (Open API)", key: "open-api", url: baseUrl + "/openapi/petstore.json"},
        {name: "Api/test001 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Api/test001/api.raml"},
        {name: "Api/test003 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Api/test003/api.raml"},
        {name: "Api/test004 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Api/test004/api.raml"},
        {name: "Fragments/test001 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Fragments/test001/fragment.raml"},
        {name: "Fragments/test004 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Fragments/test004/DataType.raml"},
        {name: "Fragments/test005 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Fragments/test005/Trait.raml"},
        {name: "Methods/test001 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Methods/test001/meth01.raml"},
        {name: "Methods/test002 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Methods/test002/meth02.raml"},
        {name: "Methods/test003 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Methods/test003/meth03.raml"},
        {name: "Resources/test001 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Resources/test001/api.raml"},
        {name: "Resources/test002 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Resources/test002/api.raml"},
        {name: "Responses/test001 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Responses/test001/api.raml"},
        {name: "Responses/test002 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Responses/test002/api.raml"},
        {name: "Responses/test003 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Responses/test003/api.raml"},
        {name: "Traits/test001 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Traits/test001/apiValid.raml"},
        {name: "Types/test001 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Types/test001/apiValid.raml"},
        {name: "Types/test003 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Types/test003/apiValid.raml"},
        {name: "Types/test004 (RAML)", key: "raml", url: baseUrl + "/raml/tck/raml-1.0/Types/test004/apiValid.raml"}
    ]);

    public selectedParserType: KnockoutObservable<any> = ko.observable<any>(this.parserTypes[0]);
    public selectedApiExample: KnockoutObservable<any> = ko.observable<any>();

    public constructor() {
        this.selectedApiExample.subscribe((newValue) => {
            if (newValue != null) {
                const parserType = this.parserTypes().find((e) => e.key == newValue.key);
                if (parserType != null) {
                    this.selectedParserType(parserType);
                }
                this.fileUrl(newValue.url);
            }
        });
    }

    public show() {
        this.fileUrl("");
        this.el().className += " is-active";
    }

    public hide() {
        const el = this.el();
        el.className = el.className.replace("is-active", "");
    }

    public cancel() {
        this.fileUrl("");
        this.hide();
    }


    public loadLocalFile() {
        /*
        (remote.getCurrentWindow() as ApiModellerWindow).checkFile((err, fileName) => {
            if (err == null && fileName != null) {
                this.fileUrl(fileName);
            }
        });
        */
    }

    public save() {
        this.emit(LoadModal.LOAD_FILE_EVENT, new LoadFileEvent(this.selectedParserType().key, this.fileUrl()));
        this.hide();
    }

    private listeners: ((e: LoadFileEvent) => undefined)[] = [];

    on(evt, listener) {
        this.listeners.push(listener);
    }

    emit(evtName: string, evt: LoadFileEvent) {
        this.listeners.forEach(l => l(evt))
    }

    public el() {
        const el = document.getElementById("load-modal");
        if (el != null) {
            return el;
        } else {
            throw new Error("Cannot find load-model modal element");
        }
    }
}
