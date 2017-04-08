var load = function (location, cb) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', location);
    xhr.onload = function () {
        if (xhr.status === 200) {
            cb(null, xhr.responseText);
        }
        else {
            cb(xhr.status, null);
        }
    };
    xhr.send();
};

module.exports.load = load;
