import fs from 'node:fs'
import path from 'node:path'
import zlib from 'node:zlib'

function findEocd(buf) {
	for (let i = buf.length - 22; i >= Math.max(0, buf.length - 65558); i--) {
		if (buf.readUInt32LE(i) === 0x06054b50) {
			return {
				entryCount: buf.readUInt16LE(i + 10),
				cdOffset: buf.readUInt32LE(i + 16),
			}
		}
	}
	return null
}

export function extractZip(zipPath, destDir) {
	const buf = fs.readFileSync(zipPath)
	const eocd = findEocd(buf)
	if (!eocd) throw new Error('Invalid ZIP: EOCD not found')

	const { entryCount, cdOffset } = eocd
	let ptr = cdOffset

	for (let i = 0; i < entryCount; i++) {
		if (buf.readUInt32LE(ptr) !== 0x02014b50) throw new Error('Corrupt central directory')
		const method = buf.readUInt16LE(ptr + 10)
		const compressedSize = buf.readUInt32LE(ptr + 20)
		const nameLen = buf.readUInt16LE(ptr + 28)
		const extraLen = buf.readUInt16LE(ptr + 30)
		const commentLen = buf.readUInt16LE(ptr + 32)
		const localOffset = buf.readUInt32LE(ptr + 42)
		const name = buf.toString('utf8', ptr + 46, ptr + 46 + nameLen)
		ptr += 46 + nameLen + extraLen + commentLen

		if (compressedSize === 0xffffffff || localOffset === 0xffffffff) {
			throw new Error('ZIP64 not supported')
		}

		const target = path.join(destDir, name)
		// Never write outside destDir ("../" or absolute entry names).
		if (path.relative(destDir, target).startsWith('..') || path.isAbsolute(path.relative(destDir, target))) continue
		if (name.endsWith('/')) {
			fs.mkdirSync(target, { recursive: true })
			continue
		}

		if (buf.readUInt32LE(localOffset) !== 0x04034b50) throw new Error('Corrupt local header')
		const lNameLen = buf.readUInt16LE(localOffset + 26)
		const lExtraLen = buf.readUInt16LE(localOffset + 28)
		const dataStart = localOffset + 30 + lNameLen + lExtraLen
		const raw = buf.subarray(dataStart, dataStart + compressedSize)
		const content = method === 0 ? raw : zlib.inflateRawSync(raw)

		fs.mkdirSync(path.dirname(target), { recursive: true })
		fs.writeFileSync(target, content)
	}
}
