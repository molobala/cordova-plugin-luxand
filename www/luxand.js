var exec = require('cordova/exec');

exports.init = function (config, success, error) {
    let licence = config.licence;
    let dbname = config.dbname;
    let tryCount = config.loginTryCount;
    console.log(config);
    exec(success, error, 'Luxand', 'init', [licence, dbname, tryCount]);
};
exports.register = function (params, success, error) {
    exec((data)=>{
        console.log("data:"+JSON.stringify(data));
        delete data.error;
        //data.extra = JSON.parse(data.extra || "{}");
        if(data.status==="SUCCESS") {
            return success(data);
        }else {
            return error(data);
        }
    }, error, 'Luxand', 'register', [params.timeout || -1]);
};
exports.login = function (params, success, error) {
    exec((data)=>{
        delete data.error;
        //data.extra = JSON.parse(data.extra || "{}");
        if(data.status==="SUCCESS") {
            return success(data);
        }else {
            return error(data);
        }
    }, error, 'Luxand', 'login', [params.timeout || -1]);
};
exports.clear = function (id, success, error) {
    exec((data)=>{
        delete data.error;
        //data.extra = JSON.parse(data.extra || "{}");
        if(data.status==="SUCCESS") {
            return success(data);
        }else {
            return error(data);
        }
    }, error, 'Luxand', 'clear', [id]);
};
exports.clearMemory = function (success, error) {
    exec((data)=>{
        delete data.error;
        //data.extra = JSON.parse(data.extra || "{}");
        if(data.status==="SUCCESS") {
            return success(data);
        }else {
            return error(data);
        }
    }, error, 'Luxand', 'clearMemory', []);
};
