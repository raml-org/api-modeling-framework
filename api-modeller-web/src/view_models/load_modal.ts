import * as ko from "knockout";
import {EventEmitter} from "events";
import {remote} from "electron";
import {ApiModellerWindow} from "../main/api_modeller_window";

export type ParserType = "raml" | "open-api";
export class LoadFileEvent {
    constructor(public type: ParserType, public location: string) {}
}

export class LoadModal extends EventEmitter{
    static LOAD_FILE_EVENT: string = "load-file";
    public fileUrl: KnockoutObservable<string> = ko.observable<string>("");
    public parserTypes: KnockoutObservableArray<any> = ko.observableArray([
        {name: "RAML 1.0", key: "raml"},
        {name: "OpenAPI 2.0", key: "open-api"}
    ]);
    public selectedParserType: KnockoutObservable<any> = ko.observable<any>(this.parserTypes[0]);

    constructor() { super() }

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
        (remote.getCurrentWindow() as ApiModellerWindow).checkFile((err, fileName) => {
            if (err == null && fileName != null) {
                this.fileUrl(fileName);
            }
        });
    }

    public save() {
        (remote.getCurrentWindow() as ApiModellerWindow).existsFile(this.fileUrl(), (err, exists) => {
            if (err == null && exists != null) {
                this.emit(LoadModal.LOAD_FILE_EVENT, new LoadFileEvent(this.selectedParserType().key,this.fileUrl()));
            } else {
                alert(`Cannot find file ${this.fileUrl()}`);
            }
            this.hide();
        });
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
