const VERSION = 5
const SIZE = 17 + VERSION * 4
const DATA_CODEWORDS = 108
const ECC_CODEWORDS = 26

type Matrix = Array<Array<boolean | null>>

class BitBuffer {
  bits: number[] = []

  append(value: number, length: number) {
    for (let i = length - 1; i >= 0; i--) {
      this.bits.push((value >>> i) & 1)
    }
  }

  toCodewords() {
    const result: number[] = []
    for (let i = 0; i < this.bits.length; i += 8) {
      let value = 0
      for (let j = 0; j < 8; j++) {
        value = (value << 1) | (this.bits[i + j] || 0)
      }
      result.push(value)
    }
    return result
  }
}

function createMatrix(): Matrix {
  return Array.from({ length: SIZE }, () => Array.from({ length: SIZE }, () => null))
}

function setModule(matrix: Matrix, row: number, col: number, dark: boolean) {
  if (row >= 0 && row < SIZE && col >= 0 && col < SIZE) {
    matrix[row][col] = dark
  }
}

function drawFinder(matrix: Matrix, row: number, col: number) {
  for (let r = -1; r <= 7; r++) {
    for (let c = -1; c <= 7; c++) {
      const rr = row + r
      const cc = col + c
      if (rr < 0 || rr >= SIZE || cc < 0 || cc >= SIZE) continue
      const dark = r >= 0 && r <= 6 && c >= 0 && c <= 6 && (r === 0 || r === 6 || c === 0 || c === 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4))
      setModule(matrix, rr, cc, dark)
    }
  }
}

function drawAlignment(matrix: Matrix, row: number, col: number) {
  for (let r = -2; r <= 2; r++) {
    for (let c = -2; c <= 2; c++) {
      setModule(matrix, row + r, col + c, Math.max(Math.abs(r), Math.abs(c)) !== 1)
    }
  }
}

function drawFunctionPatterns(matrix: Matrix) {
  drawFinder(matrix, 0, 0)
  drawFinder(matrix, 0, SIZE - 7)
  drawFinder(matrix, SIZE - 7, 0)
  drawAlignment(matrix, 30, 30)

  for (let i = 8; i < SIZE - 8; i++) {
    const dark = i % 2 === 0
    setModule(matrix, 6, i, dark)
    setModule(matrix, i, 6, dark)
  }

  setModule(matrix, SIZE - 8, 8, true)
  drawFormatBits(matrix)
}

function formatBits() {
  // Error correction L, mask 0.
  let data = 0b01000
  let value = data << 10
  const generator = 0b10100110111
  for (let i = 14; i >= 10; i--) {
    if (((value >>> i) & 1) !== 0) {
      value ^= generator << (i - 10)
    }
  }
  return ((data << 10) | value) ^ 0b101010000010010
}

function drawFormatBits(matrix: Matrix) {
  const bits = formatBits()
  const bit = (index: number) => ((bits >>> index) & 1) === 1

  for (let i = 0; i <= 5; i++) setModule(matrix, 8, i, bit(i))
  setModule(matrix, 8, 7, bit(6))
  setModule(matrix, 8, 8, bit(7))
  setModule(matrix, 7, 8, bit(8))
  for (let i = 9; i < 15; i++) setModule(matrix, 14 - i, 8, bit(i))

  for (let i = 0; i < 8; i++) setModule(matrix, SIZE - 1 - i, 8, bit(i))
  for (let i = 8; i < 15; i++) setModule(matrix, 8, SIZE - 15 + i, bit(i))
}

function encodeText(text: string) {
  const bytes = Array.from(new TextEncoder().encode(text))
  if (bytes.length > 106) {
    throw new Error('二维码内容过长')
  }

  const buffer = new BitBuffer()
  buffer.append(0b0100, 4)
  buffer.append(bytes.length, 8)
  bytes.forEach((byte) => buffer.append(byte, 8))

  const capacityBits = DATA_CODEWORDS * 8
  buffer.append(0, Math.min(4, capacityBits - buffer.bits.length))
  while (buffer.bits.length % 8 !== 0) buffer.append(0, 1)

  const data = buffer.toCodewords()
  const pads = [0xec, 0x11]
  for (let i = 0; data.length < DATA_CODEWORDS; i++) {
    data.push(pads[i % 2])
  }
  return data
}

function gfMultiply(x: number, y: number) {
  let result = 0
  for (let i = 7; i >= 0; i--) {
    result = (result << 1) ^ ((result >>> 7) * 0x11d)
    if (((y >>> i) & 1) !== 0) result ^= x
  }
  return result & 0xff
}

function reedSolomonGenerator(degree: number) {
  const result = Array.from({ length: degree }, () => 0)
  result[degree - 1] = 1
  let root = 1
  for (let i = 0; i < degree; i++) {
    for (let j = 0; j < degree; j++) {
      result[j] = gfMultiply(result[j], root)
      if (j + 1 < degree) {
        result[j] ^= result[j + 1]
      }
    }
    root = gfMultiply(root, 2)
  }
  return result
}

function reedSolomonRemainder(data: number[]) {
  const generator = reedSolomonGenerator(ECC_CODEWORDS)
  const result = Array.from({ length: ECC_CODEWORDS }, () => 0)
  data.forEach((byte) => {
    const factor = byte ^ result.shift()!
    result.push(0)
    generator.forEach((coef, index) => {
      result[index] ^= gfMultiply(coef, factor)
    })
  })
  return result
}

function drawCodewords(matrix: Matrix, codewords: number[]) {
  const bits = codewords.flatMap((byte) => Array.from({ length: 8 }, (_, i) => (byte >>> (7 - i)) & 1))
  let bitIndex = 0
  let upward = true

  for (let right = SIZE - 1; right >= 1; right -= 2) {
    if (right === 6) right--
    for (let vert = 0; vert < SIZE; vert++) {
      const row = upward ? SIZE - 1 - vert : vert
      for (let j = 0; j < 2; j++) {
        const col = right - j
        if (matrix[row][col] !== null) continue
        const raw = bitIndex < bits.length ? bits[bitIndex] === 1 : false
        const masked = raw !== ((row + col) % 2 === 0)
        matrix[row][col] = masked
        bitIndex++
      }
    }
    upward = !upward
  }
}

function escapeXml(value: string) {
  return value.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;')
}

export function createQrSvg(text: string, moduleSize = 6) {
  const matrix = createMatrix()
  drawFunctionPatterns(matrix)
  const data = encodeText(text)
  drawCodewords(matrix, data.concat(reedSolomonRemainder(data)))

  const quiet = 4
  const size = (SIZE + quiet * 2) * moduleSize
  const rects: string[] = []
  matrix.forEach((row, r) => {
    row.forEach((dark, c) => {
      if (dark) {
        rects.push(`<rect x="${(c + quiet) * moduleSize}" y="${(r + quiet) * moduleSize}" width="${moduleSize}" height="${moduleSize}"/>`)
      }
    })
  })

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" role="img" aria-label="${escapeXml(text)}"><rect width="100%" height="100%" fill="#fff"/>` +
    `<g fill="#111827">${rects.join('')}</g></svg>`
}

export function createQrDataUrl(text: string, moduleSize = 6) {
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(createQrSvg(text, moduleSize))}`
}
