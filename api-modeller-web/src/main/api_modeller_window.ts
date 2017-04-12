import {ModelProxy} from "./model_proxy";
export type ModelType = "raml" | "open-api" | "api-model";
export class ApiModellerWindow {

    private apiFramework = window['api_modelling_framework'].core;

    static functions: string[] = [
        "checkFile", "existsFile", "parseModelFile", "generateString"
    ];

    checkFile(cb) {
        /*
        Electron.dialog.showOpenDialog((fileNames) => {
            const fileName = (fileNames || [])[0];
            if (fileName != null) {
                console.log(`Loading fileName ${fileName}`);
                this.existsFile(fileName, cb);
            } else {
                cb(null, null)
            }
        });
        */
    };

    existsFile(fileName, cb) {
        /*
        if (fs.existsSync(fileName)) {
            cb(null, fileName);
        } else {
            cb(new Error("File does not exists"), null);
        }
        */
        cb(null, fileName);
    }

    parseModelFile(type: ModelType, fileLocation: string, cb) {

        console.log("PARSING FILE " + fileLocation + " TYPE " + type);
        let parser: any;
        if (type === "raml") {
            parser = new this.apiFramework.__GT_RAMLParser();
        } else if(type === "open-api") {
            parser = new this.apiFramework.__GT_OpenAPIParser();
        }

        this.apiFramework.parse_file(parser, fileLocation, function(err, model) {
            if (err != null) {
                console.log("Error parsing file");
                console.log(err);
                cb(err, null);
            } else {
                cb(null, new ModelProxy(model, type))
            }
        })
    }
    from_clj(x: any) {
        return this.apiFramework.fromClj(x)
    }

    to_clj(x: any) {
        console.log("********** TO CLJ");
        try {
            return this.apiFramework.toClj(x)
        } catch (e) {
            console.log("ERROR");
            console.log(e);
            return x;
        }
    }

    parseString(type: ModelType, baseUrl: string, value: string, cb: (err, model) => any) {
        let parser: any;
        if (type === "raml") {
            parser = new this.apiFramework.__GT_RAMLParser();
        } else if(type === "open-api") {
            parser = new this.apiFramework.__GT_OpenAPIParser();
        } else if(type === "api-model") {
            parser = new this.apiFramework.__GT_APIModelParser();
        }

        try {
            this.apiFramework.parse_string(parser, baseUrl, value, function (err, model) {
                if (err != null) {
                    console.log("Error parsing text");
                    console.log(err);
                    cb(err, null);
                } else {
                    cb(null, new ModelProxy(model, type))
                }
            })
        } catch (e) {
            cb(e, null);
        }
    }
}
