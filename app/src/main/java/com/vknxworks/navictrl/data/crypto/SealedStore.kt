package com.vknxworks.navictrl.data.crypto

import java.io.File

/**
 * One small file of the app's own data (a JSON list) kept encrypted at rest with the data key
 * ([DataKeyStore], [SealedFile]).
 *
 * - A file from before encryption is read as it is and then replaced by its encrypted copy
 *   ([SealedFile.sealInPlace]: written next to it, read back, compared, renamed over it).
 * - Without a data key (the phone's Keystore doesn't work) the ordinary file is read and written, as before.
 * - An encrypted file that can't be opened (the key is gone or doesn't match, or the file is damaged) makes
 *   the store [unreadable]: the file stays exactly as it is and nothing is written over it until it opens
 *   again ([read]), is set aside ([setAside]) or everything is wiped on purpose ([clear]).
 *
 * Every call here reads or writes the disk and may ask the Keystore for the key: never call it on the main thread.
 */
class SealedStore(private val file: File, private val keys: DataKeyStore) {
    private val tmp = File(file.parentFile, file.name + ".tmp")
    private val sealTmp = File(file.parentFile, file.name + ".seal.tmp")

    /** The file was looked at in this run, so it's known whether it may be written over. */
    private var checked = false

    /** The file is encrypted and can't be opened. It is kept, and [write] does nothing. */
    @Volatile
    var unreadable: Boolean = false
        private set

    /** The file's contents, or null when there is no file or it's [unreadable]. */
    @Synchronized
    fun read(): ByteArray? {
        if (!file.exists()) {
            unreadable = false
            checked = true
            return null
        }
        val bytes = try {
            file.readBytes()
        } catch (e: Exception) {
            // It's there but can't be read right now: don't write over it.
            unreadable = true
            checked = true
            return null
        }
        if (!SealedFile.isSealed(bytes)) {
            unreadable = false
            checked = true
            // From before encryption: seal it now. On any failure it stays the ordinary file it was.
            val key = keys.get(create = true)
            if (key != null) SealedFile.sealInPlace(key, file, sealTmp)
            return bytes
        }
        val key = keys.get(create = false)
        val plain = if (key == null) null else try {
            SealedFile.open(key, bytes)
        } catch (e: Exception) {
            null
        }
        unreadable = plain == null
        checked = true
        return plain
    }

    /**
     * Saves [plain]: encrypted when there's a data key, as an ordinary file otherwise. Returns false when
     * nothing was written (the file on disk is [unreadable], or writing failed).
     */
    @Synchronized
    fun write(plain: ByteArray): Boolean {
        // Never over a file nobody has looked at yet: it could be one that can't be opened.
        if (!checked) read()
        if (unreadable) return false
        return try {
            val key = keys.get(create = true)
            val sealed = if (key == null) null else SealedFile.sealChecked(key, plain)
            SealedFile.writeAtomic(file, tmp, sealed ?: plain)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gives up on a file that can't be opened without deleting it: it is renamed to `<name>.unreadable`
     * (an earlier one keeps its place; this one then gets the time added). Does nothing unless [unreadable].
     * Returns true when the store can be used again.
     */
    @Synchronized
    fun setAside(): Boolean {
        if (!checked) read()
        if (!unreadable) return true
        try {
            if (file.exists()) {
                var aside = File(file.parentFile, file.name + ".unreadable")
                if (aside.exists()) aside = File(file.parentFile, file.name + ".unreadable." + System.currentTimeMillis())
                if (!file.renameTo(aside)) return false
            }
            unreadable = false
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /** Wipes the file on purpose ("Forgot PIN?" removes everything the app saved), readable or not. */
    @Synchronized
    fun clear() {
        try {
            file.delete()
            tmp.delete()
            sealTmp.delete()
        } catch (e: Exception) {
        }
        // Still there (it couldn't be deleted): look at it again before anything is written.
        if (file.exists()) {
            checked = false
        } else {
            unreadable = false
            checked = true
        }
    }
}
