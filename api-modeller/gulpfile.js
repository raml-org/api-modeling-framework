'use strict';

var gulp = require('gulp');
var electron = require('electron-connect').server.create();

gulp.task('serve', function () {

    // Start browser process
    electron.start();

    // Restart browser process
    gulp.watch('src/main.js', electron.restart);

    // Reload renderer process
    gulp.watch(['src/**/*.js', 'public/**/*.html', 'public/**/*.css'], electron.reload);
});