import * as ko from "knockout";
export class LoadFileEvent {
    constructor(type, location) {
        this.type = type;
        this.location = location;
    }
}
export class LoadModal {
    constructor() {
        this.fileUrl = ko.observable("");
        this.parserTypes = ko.observableArray([
            { name: "RAML 1.0", key: "raml" },
            { name: "OpenAPI 2.0", key: "open-api" }
        ]);
        this.selectedParserType = ko.observable(this.parserTypes[0]);
        this.listeners = [];
    }
    show() {
        this.fileUrl("");
        this.el().className += " is-active";
    }
    hide() {
        const el = this.el();
        el.className = el.className.replace("is-active", "");
    }
    cancel() {
        this.fileUrl("");
        this.hide();
    }
    loadLocalFile() {
        /*
        (remote.getCurrentWindow() as ApiModellerWindow).checkFile((err, fileName) => {
            if (err == null && fileName != null) {
                this.fileUrl(fileName);
            }
        });
        */
    }
    save() {
        /*
        (remote.getCurrentWindow() as ApiModellerWindow).existsFile(this.fileUrl(), (err, exists) => {
            if (err == null && exists != null) {
                this.emit(LoadModal.LOAD_FILE_EVENT, new LoadFileEvent(this.selectedParserType().key,this.fileUrl()));
            } else {
                alert(`Cannot find file ${this.fileUrl()}`);
            }
            this.hide();
        });
        */
    }
    on(evt, listener) {
        this.listeners.push(listener);
    }
    el() {
        const el = document.getElementById("load-modal");
        if (el != null) {
            return el;
        }
        else {
            throw new Error("Cannot find load-model modal element");
        }
    }
}
LoadModal.LOAD_FILE_EVENT = "load-file";
//# sourceMappingURL=load_modal.js.map