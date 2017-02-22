import * as Electron from "electron";
import * as fs from "fs";
import {ModelProxy, ModelLevel} from "./model_proxy";
require("api_modelling_framework");

const apiFramework = global["api_modelling_framework"].core;

export type ModelType = "raml" | "open-api";

export class ApiModellerWindow extends Electron.BrowserWindow {

    static functions: string[] = [
        "checkFile", "existsFile", "parseModelFile", "from_clj",
        "to_clj", "generateString"
    ];

    static wrap(o: Object) {
        ApiModellerWindow.functions.forEach(f => {
            o[f] = ApiModellerWindow.prototype[f];
        });
    }

    checkFile(cb) {
        Electron.dialog.showOpenDialog((fileNames) => {
            const fileName = (fileNames || [])[0];
            if (fileName != null) {
                console.log(`Loading fileName ${fileName}`);
                this.existsFile(fileName, cb);
            } else {
                cb(null, null)
            }
        });
    };

    existsFile(fileName, cb) {
        if (fs.existsSync(fileName)) {
            cb(null, fileName);
        } else {
            cb(new Error("File does not exists"), null);
        }
    }

    from_clj(x: any) {
        apiFramework.from_clj(x)
    }

    to_clj(x: any) {
        apiFramework.to_clj(x)
    }

    parseModelFile(type: ModelType, fileLocation: string, cb) {
        let parser: any;
        if (type === "raml") {
            parser = new apiFramework.RAMLParser();
        } else if(type === "open-api") {
            parser = new apiFramework.OpenAPIParser();
        }

        var that = this;
        apiFramework.parse_file(parser, fileLocation, function(err, model) {
            if (err != null) {
                cb(err, null);
            } else {
                cb(null, new ModelProxy(model, type))
            }
        })
    }
}
