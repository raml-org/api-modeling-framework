import * as ko from "knockout";
import {ModelLevel} from "../main/model_proxy";
import {EventEmitter} from "events";

export class Nav extends EventEmitter{
    static DOCUMENT_LEVEL_SELECTED_EVENT = "document_level_selected_event";
    public documentLevelOptions: KnockoutObservableArray<any> = ko.observableArray<any>([
        {name: "Document Model", key: "document"},
        {name: "Domain Model", key: "domain"}
    ]);
    public selectedDocumentLevel: KnockoutObservable<any> = ko.observable<any>(this.documentLevelOptions[0]);

    constructor(level: ModelLevel) {
        super();
        this.selectedDocumentLevel(this.documentLevelOptions.peek().filter(e => e.key === level));
        this.selectedDocumentLevel.subscribe((v) => {
            if (v != null) {
                this.emit(Nav.DOCUMENT_LEVEL_SELECTED_EVENT, v.key)
            }
        });
    }
}