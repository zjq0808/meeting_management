const { defineConfig } = require('@vue/cli-service')
const path = require('path')

module.exports = defineConfig({
  transpileDependencies: false,
  publicPath: './',
  outputDir: 'dist',
  productionSourceMap: false,
  lintOnSave: false,
  chainWebpack: (config) => {
    config.module
      .rule('js')
      .exclude.add(path.resolve(__dirname, 'src/api/wxcommon.min.gwt.js'))

    if (process.env.NODE_ENV === 'development') {
      config.plugins.delete('fork-ts-checker')
    }
  },
  devServer: {
    port: 8082,
    historyApiFallback: true,
    proxy: {
      '/api': {
        target: process.env.VUE_APP_API_PROXY || 'http://localhost:9090',
        changeOrigin: true
      }
    }
  }
})
