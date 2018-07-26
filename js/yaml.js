var fs = (typeof (NODE_FS) === 'undefined' ? { readFile: function () { throw new Error("readFile only supported in node.js") } } : NODE_FS);
var yaml = require('./js-yaml/index.js');
var path = require("path");
var rest = require("rest");
var json_ast = require("./json_ast");

global.FRAGMENTS_CACHE = {};
global.PENDING_LIBRARIES = [];
global.SHACL = require("shacl").validate;

var ensureFileUri = function (uri) {
    if (uri.indexOf("://") === -1) {
        uri = "file://" + uri;
    }
    return uri;
};

var startsWith = function (s, p) {
    return s.indexOf(p) === 0;
};

var isLocalFile = function (location) {
    return (startsWith(location, "file://") || location.indexOf("://") === -1);
};

var inCache = function (location, cacheDirs) {
    for (var domain in cacheDirs || {}) {
        if (startsWith(location, domain)) {
            return location.replace(domain, cacheDirs[domain]);
        }
    }
};

var resolveFile = function (location, cb) {
    if (isLocalFile(location)) {
        fs.readFile(location.replace("file://", ""), function (err, data) {
            if (err) {
                cb(err);
            } else {
                cb(null, data.toString());
            }
        });
    } else if (inCache(location, global.PARSING_OPTIONS.cacheDirs || {})) {
        resolveFile(inCache(location, global.PARSING_OPTIONS.cacheDirs || {}), cb);
    } else {
        global.JS_REST(location).then(function (response) {
            cb(null, response.entity);
        }).catch(function (err) {
            cb(err);
        });
    }
};

var resolvePath = function (location, path) {
    var lastComponent = location.split("/").pop();
    var base = location.replace(lastComponent, "");
    if (path.indexOf("/") === 0 || path.indexOf("://") !== -1) {
        //console.log("RESOLVING PATH (" + base + " + " + path + ") --> " + path);
        return path;
    } else {
        //console.log("RESOLVING PATH (" + base + " + " + path + ") --> " + (base + path));
        return base + path;
    }
};

var updateFragments = function (file, data, pending, cb) {
    var matches = data.match(/!include\s+(.+)/g) || [];
    var files = matches.map(function (f) {
        var filePath = f.split(/!include\s+/)[1];
        filePath = filePath.replace(/'|"$/, "");
        var location = resolvePath(file.location, filePath);
        return {
            "path": filePath,
            "location": location
        };
    });
    pending = pending.concat(files);

    FRAGMENTS_CACHE[file.path || file.location] = {
        "data": data,
        "location": file.location
    };

    if (pending.length === 0) {
        cb(null, FRAGMENTS_CACHE);
    } else {
        var next = pending.shift();
        cacheFragments(next, cb, pending);
    }
};

var cacheFragments = function (fileOrData, cb, pending) {
    if (pending == null) {
        pending = [];
    }

    if (fileOrData.data != null) {
        updateFragments(fileOrData, fileOrData.data, pending, cb);
    } else {
        // console.log(fileOrData);
        if (typeof (AMF_LOADING_EVENT) !== 'undefined') {
            try {
                AMF_LOADING_EVENT(fileOrData.location);
            } catch (e) { }
        }
        resolveFile(fileOrData.location, function (err, data) {
            if (err) {
                cb(err);
            } else {
                updateFragments(fileOrData, data, pending, cb);
            }
        });
    }

};

var loadLibraries = function (loaded, cb, pending) {
    if (pending == null) {
        pending = PENDING_LIBRARIES;
        PENDING_LIBRARIES = [];
        loadLibraries(loaded, cb, pending);
    } else {
        if (pending.length == 0) {
            cb(null, loaded);
        } else {
            var next = pending.shift();
            parseYamlFile(next.location, global.PARSING_OPTIONS, function (err, loadedFragment) {
                if (err) {
                    cb(err, loaded);
                } else {
                    loaded.uses[next.alias] = loadedFragment;
                    loadLibraries(loaded, cb, pending);
                }
            });
        }
    }
};

var getFragmentInfo = function (fragment) {
    var fragmentInfo = fragment.data.split("\n")[0];
    if (fragmentInfo.indexOf("#%RAML") === 0) {
        return fragmentInfo;

    } else {
        return null;
    }

};

var collectLibraries = function (fragment, location) {
    var libraries = fragment.uses || {};
    for (var p in libraries) {
        if (p !== "__location__") {
            var value = libraries[p];
            if (typeof (value) === "object" && value["amf-lexical-token"]) {
                value = value["amf-lexical-token"];
            }
            var resolvedLocation = resolvePath(location, value);
            PENDING_LIBRARIES.push({
                "path": libraries[p],
                "location": resolvedLocation,
                "alias": p
            });
        }
    }
};

var FragmentType = new yaml.Type("!include", {
    kind: "scalar",

    resolve: function (file) {
        return FRAGMENTS_CACHE[file] != null;
    },

    construct: function (file) {
        var fragment = FRAGMENTS_CACHE[file];
        var parsed = null;
        // console.log("Trying to parse inner '" + fragment.data.substr(0, 10) + "'");

        try {
            parsed = yaml.load(fragment.data, { schema: FRAGMENT_SCHEMA });
        } catch (e) {
            parsed = fragment.data;
        }
        var fragmentInfo = getFragmentInfo(fragment);

        var location = fragment.location;

        return {
            "@location": ensureFileUri(location),
            "@fragment": fragmentInfo,
            "@data": parsed,
            "@raw": fragment.data
        };
    }
});

var FRAGMENT_SCHEMA = yaml.Schema.create([FragmentType]);

var parseYamlFile = function (location, options, cb) {
    global.PARSING_OPTIONS = options || {};
    cacheFragments({ "location": location }, function (err, data) {
        try {
            var loaded = yaml.load(FRAGMENTS_CACHE[location].data, { schema: FRAGMENT_SCHEMA });
            collectLibraries(loaded, location);

            loadLibraries(loaded, function (err, loaded) {
                var result = { "@data": loaded };
                result["@location"] = ensureFileUri(location);
                result["@fragment"] = getFragmentInfo(FRAGMENTS_CACHE[location]);
                result["@raw"] = FRAGMENTS_CACHE[location].data;
                cb(null, result);
            });
        } catch (e) {
            cb(e);
        }
    });
};

var parseYamlString = function (location, data, options, cb) {
    global.PARSING_OPTIONS = options || {};
    cacheFragments({ "location": location, "data": data }, function (err, data) {
        try {
            var loaded = yaml.load(FRAGMENTS_CACHE[location].data, { schema: FRAGMENT_SCHEMA });
            collectLibraries(loaded, location);
            loadLibraries(loaded, function (err, loaded) {
                if (err == null) {
                    var result = { "@data": loaded };
                    result["@location"] = ensureFileUri(location);
                    result["@fragment"] = getFragmentInfo(FRAGMENTS_CACHE[location]);
                    result["@raw"] = FRAGMENTS_CACHE[location].data;
                    cb(null, result);
                } else {
                    cb(err);
                }

            });
        } catch (e) {
            cb(e);
        }
    });
};

module.exports.parseYamlFile = parseYamlFile;
module.exports.parseYamlString = parseYamlString;
module.exports.loadYaml = yaml.load;
module.exports.dump = yaml.dump;
global.JS_YAML = {};
global.JS_YAML.loadYaml = yaml.load;
global.JS_YAML.parseYamlFile = parseYamlFile;
global.JS_YAML.parseYamlString = parseYamlString;
global.JS_YAML.dump = yaml.dump;
global.JS_REST = function (location) {
    if (typeof (global.JS_USE_PROXY) !== "undefined") {
        location = global.JS_USE_PROXY.replace("$URL", encodeURIComponent(location));
    }
    return rest(location);
};
