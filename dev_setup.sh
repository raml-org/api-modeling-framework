#!/bin/bash

lein npm install
rm -rf node
lein clean
# lein cljsbuild once default
# cp -rf ./package_files/* ./
lein cljsbuild once web
ln -s $(pwd)/js $(pwd)/node/js
cd api-modeller
tsc
npm install
cd ..
ln -s $(pwd) $(pwd)/api-modeller/node_modules/api_modeling_framework
pushd api-modeller/public
bower install
popd
cd api-modeller && gulp serve
