var fs = require('fs');
var p = require('path');
var yaml = require('js-yaml');
var path = require("path");

global.FRAGMENTS_CACHE = {};

function Fragment(location, type, data) {
    this["@location"] = location;
    this["@fragment"] = type;
    this["@data"] = data;
}

var resolveFile = function (location, cb) {
    fs.readFile(location, function (err, data) {
        if (err) {
            cb(err);
        } else {
            cb(null, data.toString());
        }
    });
};

var resolvePath = function (location, path) {
    var lastComponent = location.split("/").pop();
    var base = location.replace(lastComponent, "");
    if (path.indexOf("/") === 0 || path.indexOf("://") !== -1) {
        // console.log("RESOLVING PATH (" + base + " + " + path + ") --> " + path);
        return path;
    } else {
        // console.log("RESOLVING PATH (" + base + " + " + path + ") --> " + (base + path));
        return base + path;
    }
};

var updateFragments = function (file, data, pending, cb) {
    var matches = data.match(/!include\s+(.+)/g) || [];
    var files = matches.map(function (f) {
        var filePath = f.split(/!include\s+/)[1];
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
        resolveFile(fileOrData.location, function (err, data) {
            if (err) {
                cb(err);
            } else {
                updateFragments(fileOrData, data, pending, cb);
            }
        });
    }

};

var getFragmentInfo = function (fragment) {
    var fragmentInfo = fragment.data.split("\n")[0];
    if (!fragmentInfo.indexOf("#%RAML") === 0) {
        fragmentInfo = null;
    }
    return fragmentInfo;
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

        // return new Fragment(location, fragmentInfo, parsed);
        return {
            "@location": location,
            "@fragment": fragmentInfo,
            "@data": parsed
        };
    }
});

var FRAGMENT_SCHEMA = yaml.Schema.create([FragmentType]);

var parseYamlFile = function (location, cb) {
    global.FRAGMENTS_CACHE = {};
    cacheFragments({ "location": location }, function (err, data) {
        try {
            var loaded = yaml.load(FRAGMENTS_CACHE[location].data, { schema: FRAGMENT_SCHEMA });
            var result = { "@data": loaded };
            result["@location"] = location;
            result["@fragment"] = getFragmentInfo(FRAGMENTS_CACHE[location]);
            cb(null, result);
        } catch (e) {
            cb(e);
        }
    });
};

var parseYamlString = function (location, data, cb) {
    global.FRAGMENTS_CACHE = {};
    cacheFragments({ "location": location, "data": data }, function (err, data) {
        try {
            var loaded = yaml.load(FRAGMENTS_CACHE[location].data, { schema: FRAGMENT_SCHEMA });
            var result = { "@data": loaded };
            result["@location"] = location;
            result["@fragment"] = getFragmentInfo(FRAGMENTS_CACHE[location]);
            cb(null, result);
        } catch (e) {
            cb(e);
        }
    });
};

module.exports.parseYamlFile = parseYamlFile;
module.exports.parseYamlString = parseYamlString;
