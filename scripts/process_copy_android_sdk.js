#!/usr/bin/env node
'use strict';

var fs = require('fs');
var path = require('path');

fs.ensureDirSync = function (dir) {
    if (!fs.existsSync(dir)) {
        dir.split(path.sep).reduce(function (currentPath, folder) {
            currentPath += folder + path.sep;
            if (!fs.existsSync(currentPath)) {
                fs.mkdirSync(currentPath);
            }
            return currentPath;
        }, '');
    }
};

 
var LUXAND_PLUGIN_DIR = "plugins/cordova-plugin-luxand";

var PLUGINS = {
    LUXAND: [
         {
            dest: LUXAND_PLUGIN_DIR + '/armeabi-v7a-libfsdk.so',
            src: "armeabi-v7a-libfsdk.so"
         },
         {
            dest: LUXAND_PLUGIN_DIR + '/arm64-v8a-libfsdk.so',
            src: "arm64-v8a-libfsdk.so"
         },
         {
            dest: LUXAND_PLUGIN_DIR + '/x86-libfsdk.so',
            src: "x86-libfsdk.so"
         }
    ]
};

// Copy key files to their platform specific folders
if (directoryExists(LUXAND_PLUGIN_DIR)) {
    copyKey(PLUGINS.LUXAND);
}


function copyKey(plugin, callback) {
    for (var i = 0; i < plugin.length; i++) {
        var file = plugin[i].src;
        if (fileExists(file)) {
            try {
                var contents = fs.readFileSync(file);

                try {
                    var destinationPath =  plugin[i].dest;
                    var folder = destinationPath.substring(0, destinationPath.lastIndexOf('/'));
                    fs.ensureDirSync(folder);
                    fs.writeFileSync(destinationPath, contents);
                } catch (e) {
                    // skip
                }

                callback && callback(contents);
            } catch (err) {
                console.log(err)
            }
        }
    }
}


function fileExists(path) {
    try {
        return fs.statSync(path).isFile();
    } catch (e) {
        return false;
    }
}

function directoryExists(path) {
    try {
        return fs.statSync(path).isDirectory();
    } catch (e) {
        return false;
    }
}