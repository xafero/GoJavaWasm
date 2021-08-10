package org.xafero.gojava.wasm.lib.runtime;

import java.io.File;

class FsStats {
	private final File _info;

	private Object dev = 1;
	private Object ino = 2;
	private Object mode = 3;
	private Object nlink = 4;
	private Object uid = 5;
	private Object gid = 6;
	private Object rdev = 7;
	private Object size = 8;
	private Object blksize = 9;
	private Object blocks = 10;
	private Object atimeMs = 11;
	private Object mtimeMs = 12;
	private Object ctimeMs = 13;

	public Object getDev() {
		return dev;
	}

	public void setDev(Object dev) {
		this.dev = dev;
	}

	public Object getIno() {
		return ino;
	}

	public void setIno(Object ino) {
		this.ino = ino;
	}

	public Object getMode() {
		return mode;
	}

	public void setMode(Object mode) {
		this.mode = mode;
	}

	public Object getNlink() {
		return nlink;
	}

	public void setNlink(Object nlink) {
		this.nlink = nlink;
	}

	public Object getUid() {
		return uid;
	}

	public void setUid(Object uid) {
		this.uid = uid;
	}

	public Object getGid() {
		return gid;
	}

	public void setGid(Object gid) {
		this.gid = gid;
	}

	public Object getRdev() {
		return rdev;
	}

	public void setRdev(Object rdev) {
		this.rdev = rdev;
	}

	public Object getSize() {
		return size;
	}

	public void setSize(Object size) {
		this.size = size;
	}

	public Object getBlksize() {
		return blksize;
	}

	public void setBlksize(Object blksize) {
		this.blksize = blksize;
	}

	public Object getBlocks() {
		return blocks;
	}

	public void setBlocks(Object blocks) {
		this.blocks = blocks;
	}

	public Object getAtimeMs() {
		return atimeMs;
	}

	public void setAtimeMs(Object atimeMs) {
		this.atimeMs = atimeMs;
	}

	public Object getMtimeMs() {
		return mtimeMs;
	}

	public void setMtimeMs(Object mtimeMs) {
		this.mtimeMs = mtimeMs;
	}

	public Object getCtimeMs() {
		return ctimeMs;
	}

	public void setCtimeMs(Object ctimeMs) {
		this.ctimeMs = ctimeMs;
	}

	public FsStats(File info) {
		_info = info;
	}

	public boolean isDirectory() {
		return _info.isDirectory();
	}
}
