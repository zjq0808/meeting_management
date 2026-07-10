const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  publicPath: './',
  outputDir: 'dist',
  productionSourceMap: false,
  lintOnSave: false,
  chainWebpack: (config) => {
    if (process.env.NODE_ENV === 'development') {
      config.plugins.delete('fork-ts-checker')
    }
  },
  devServer: {
    port: 8082,
    proxy: {
      '/api': {
        target: process.env.VUE_APP_API_PROXY || 'http://localhost:9090',
        changeOrigin: true
      }
    }
  }
})
