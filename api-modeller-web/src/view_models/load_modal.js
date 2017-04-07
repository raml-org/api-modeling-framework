"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const ko = require("knockout");
const events_1 = require("events");
const electron_1 = require("electron");
class LoadFileEvent {
    constructor(type, location) {
        this.type = type;
        this.location = location;
    }
}
exports.LoadFileEvent = LoadFileEvent;
class LoadModal extends events_1.EventEmitter {
    constructor() {
        super();
        this.fileUrl = ko.observable("");
        this.parserTypes = ko.observableArray([
            { name: "RAML 1.0", key: "raml" },
            { name: "OpenAPI 2.0", key: "open-api" }
        ]);
        this.selectedParserType = ko.observable(this.parserTypes[0]);
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
        electron_1.remote.getCurrentWindow().checkFile((err, fileName) => {
            if (err == null && fileName != null) {
                this.fileUrl(fileName);
            }
        });
    }
    save() {
        electron_1.remote.getCurrentWindow().existsFile(this.fileUrl(), (err, exists) => {
            if (err == null && exists != null) {
                this.emit(LoadModal.LOAD_FILE_EVENT, new LoadFileEvent(this.selectedParserType().key, this.fileUrl()));
            }
            else {
                alert(`Cannot find file ${this.fileUrl()}`);
            }
            this.hide();
        });
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
exports.LoadModal = LoadModal;
//# sourceMappingURL=load_modal.js.map