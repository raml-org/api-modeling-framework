import * as ko from "knockout";
import {ModelLevel} from "../main/model_proxy";

export class Nav {
    static DOCUMENT_LEVEL_SELECTED_EVENT = "document_level_selected_event";
    public documentLevelOptions: KnockoutObservableArray<any> = ko.observableArray<any>([
        {name: "Document Model", key: "document"},
        {name: "Domain Model", key: "domain"}
    ]);
    public selectedDocumentLevel: KnockoutObservable<any> = ko.observable<any>(this.documentLevelOptions[0]);

    constructor(level: ModelLevel) {
        this.selectedDocumentLevel(this.documentLevelOptions.peek().filter(e => e.key === level));
        this.selectedDocumentLevel.subscribe((v) => {
            if (v != null) {
                this.emit(Nav.DOCUMENT_LEVEL_SELECTED_EVENT, v.key)
            }
        });
    }

    private listeners: ((e:ModelLevel) => undefined)[] = [];

    on(evt, listener) {
        this.listeners.push(listener);
    }

    emit(evt: string, level: ModelLevel) {
        debugger;
        this.listeners.forEach(l => l(level));
    }

}