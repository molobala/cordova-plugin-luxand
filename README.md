# Cordova plugin for Luxand Face SDK
Cross-platform Face Recognition using Luxand Face SDK framework

# Supported Platforms:
This plugin supports following platforms:

* IOS
* Android

## Installation

This requires cordova 7+

`cordova plugin add cordova-plugin-luxand`

Or directrly via this repoitory :

`https://github.com/molobala/cordova-plugin-luxand.git`

Note: 
You need to put luxand binay files (downloadable [here](https://drive.google.com/open?id=11Nfjnpwsrzmf0isIMPkdtTYqWt8eG-1G)) to your project root before installation

## Usage
Before using the plugin you have to init it with your luxand licence key.

```js
Luxand.init({
    licence: "",
    loginTryCount: 3,
    dbname: "test.dat"
}, r=>{}, err=>{});
```
-  `licence` is your licence key
-  `loginTryCoun`t is the number of repetition for the login and register process
-  `dbname` is the local file faces templates will be saved in.

This plugin allow you to register a user taking it's face template and recognize him later. The registration is performed once and only once for a user.
To register a user call the `register` method on the plugin like this

```js
Luxand.register({
    timeout: 20000
}, (r)=>{
    console.log("Your FACE ID:", r.id);
    console.log("Your AGE:", r.extra.AGE);
    console.log("Your GENDER:", r.extra.GENDER);
    console.log("SIMILING:", r.extra.SMILE>35? "YES": "NO");
    console.log("EYE OPENED:", r.extra.EYESOPENED>45? "YES": "NO");
}, (err)=>{
    if(err.messgae= "Already registered") {
        //extra data available
        console.log("Your AGE:", r.extra.AGE);
        console.log("Your GENDER:", r.extra.GENDER);
        console.log("SIMILING:", r.extra.SMILE>35? "YES": "NO");
        console.log("EYE OPENED:", r.extra.EYESOPENED>45? "YES": "NO");
    }
})
```

The param `timeout` is the number of millisecond from which the plugin should return if no face detected

To register a user call the `register` method on the plugin like this

```js
Luxand.login({
    timeout: 20000
}, (r)=>{
    console.log("Your FACE ID:", r.id);
    console.log("Your AGE:", r.extra.AGE);
    console.log("Your GENDER:", r.extra.GENDER);
    console.log("SIMILING:", r.extra.SMILE>35? "YES": "NO");
    console.log("EYE OPENED:", r.extra.EYESOPENED>45? "YES": "NO");
}, (err)=>{
    if(err.extra && !err.extra.timeout) {
        //extra data available (login just fail)
        console.log("Your AGE:", r.extra.AGE);
        console.log("Your GENDER:", r.extra.GENDER);
        console.log("SIMILING:", r.extra.SMILE>35? "YES": "NO");
        console.log("EYE OPENED:", r.extra.EYESOPENED>45? "YES": "NO");
    }
});
```

The param `timeout` is the number of millisecond from which the plugin should return if no face detected


To clear the plugin memory (local file where faces templates are stored)
```js
Luxand.clearMemory((r)=>{}, err=>{});
```

To clear specific face from memory call `clear`method passing the face id you got from login or register method
```js
Luxand.clear(id,(r)=>{}, err=>{});
```

# Licence

The MIT License

Copyright (c) 2019 DOUMBIA Mahamadou (doumbiamahamadou.ensate in gmail)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.