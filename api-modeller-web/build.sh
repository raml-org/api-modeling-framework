npm install -g http-server
npm install
pushd ../
rm -f index_package.js
lein cljsbuild once web
popd
mkdir -p ./public/js
cp ../index_package.js ./public/js/amf.js
node build.js > ./public/js/api_modeller.js
cd public
http-server
