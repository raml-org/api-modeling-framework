var parse = require("json-to-ast");

var reconstructNode = function (node, lexicalInfo) {
    var acc = {};
    node.children.forEach(function (child) {
        acc[child.key.value] = reconstruct(child.value);
    });
    acc['__location__'] = lexicalInfo;
    return acc;
};

var reconstructLiteral = function (node) {
    return node.value;
};

var reconstructArray = function (node) {
    var acc = [];
    node.children.forEach(function (child) {
        acc.push(reconstruct(child));
    });
    return acc;
};

var reconstruct = function (node) {
    var location = node.loc;
    var lexicalInfo = null;
    if (location != null) {
        lexicalInfo = {
            "start-line": location.start.line,
            "start-column": location.start.column,
            "start-index": -1,
            "end-line": location.end.line,
            "end-column": location.end.column,
            "end-index": -1
        };
    }

    switch (node.type) {
        case "object": return reconstructNode(node, lexicalInfo);
        case "literal": return reconstructLiteral(node);
        case "array": return reconstructArray(node);
    }
};

var ast = function (file, text) {
    var tree = parse(text, { source: file });
    return reconstruct(tree);
};


module.exports = ast;
global.JS_AST = ast;

//console.log(JSON.stringify(ast("test.yaml", '{"a":1,"b":[{"b":3,"d":{"e":3}}, true, false]}'), null, 2));
