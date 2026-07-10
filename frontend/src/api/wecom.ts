const wxcommon = require('./wxcommon.min.gwt')

const WECOM_ENABLED = process.env.VUE_APP_WECOM_ENABLED === 'true'

export function isWeComEnabled() {
  return WECOM_ENABLED
}

export async function initializeWeCom() {
  if (!WECOM_ENABLED) {
    return
  }
  const result = await wxcommon.doInitWx('wx')
  const data = result?.data || {}
  if (data.errCode && data.errCode !== '0') {
    throw new Error(data.errMsg || '企业微信初始化失败')
  }
}

export function appendSignature(config: any) {
  if (!WECOM_ENABLED) {
    return config
  }
  const secretObject = wxcommon.signatureUrl('', config) || {}
  config.params = {
    ...(config.params || {}),
    secretkey: secretObject.secretKey
  }
  return config
}

export function encryptRequestData(config: any) {
  if (!WECOM_ENABLED || !config.data || config.data instanceof FormData) {
    return config
  }
  const method = String(config.method || '').toLowerCase()
  const contentType = String(config.headers?.['Content-Type'] || config.headers?.post?.['Content-Type'] || '')
  if (['post', 'put', 'patch'].includes(method) && (!contentType || contentType.includes('application/json'))) {
    config.data = wxcommon.enSymmetricData(config.data)
  }
  return config
}

export function decryptResponseData(respData: unknown) {
  if (!WECOM_ENABLED || typeof respData !== 'string') {
    return respData
  }
  try {
    return JSON.parse(respData)
  } catch (error) {
    return wxcommon.deSymmetricData(respData)
  }
}

export function initResponsiveRem(maxWidth = 550) {
  const update = () => {
    const width = document.documentElement.clientWidth || document.body.clientWidth || 375
    if (width > 768) {
      document.documentElement.style.fontSize = ''
      return
    }
    const baseWidth = Math.min(width, maxWidth)
    document.documentElement.style.fontSize = `${(100 * baseWidth) / 750}px`
  }
  update()
  window.addEventListener('resize', update)
}
