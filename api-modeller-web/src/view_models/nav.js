"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const ko = require("knockout");
const events_1 = require("events");
class Nav extends events_1.EventEmitter {
    constructor(level) {
        super();
        this.documentLevelOptions = ko.observableArray([
            { name: "Document Model", key: "document" },
            { name: "Domain Model", key: "domain" }
        ]);
        this.selectedDocumentLevel = ko.observable(this.documentLevelOptions[0]);
        this.selectedDocumentLevel(this.documentLevelOptions.peek().filter(e => e.key === level));
        this.selectedDocumentLevel.subscribe((v) => {
            if (v != null) {
                this.emit(Nav.DOCUMENT_LEVEL_SELECTED_EVENT, v.key);
            }
        });
    }
}
Nav.DOCUMENT_LEVEL_SELECTED_EVENT = "document_level_selected_event";
exports.Nav = Nav;
//# sourceMappingURL=nav.js.map