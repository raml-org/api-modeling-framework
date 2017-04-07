import * as ko from "knockout";
export class Nav {
    constructor(level) {
        this.documentLevelOptions = ko.observableArray([
            { name: "Document Model", key: "document" },
            { name: "Domain Model", key: "domain" }
        ]);
        this.selectedDocumentLevel = ko.observable(this.documentLevelOptions[0]);
        this.listeners = [];
        this.selectedDocumentLevel(this.documentLevelOptions.peek().filter(e => e.key === level));
        this.selectedDocumentLevel.subscribe((v) => {
            if (v != null) {
                this.emit(Nav.DOCUMENT_LEVEL_SELECTED_EVENT, v.key);
            }
        });
    }
    on(evt, listener) {
        this.listeners.push(listener);
    }
    emit(evt, level) {
        this.listeners.forEach(l => l(evt, level));
    }
}
Nav.DOCUMENT_LEVEL_SELECTED_EVENT = "document_level_selected_event";
//# sourceMappingURL=nav.js.map