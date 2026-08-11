package me.magnum.melonds.impl

import me.magnum.enhancements.EnhancementPackageInstaller

class StagedEnhancementOwner(
    private var staged: EnhancementPackageInstaller.Staged? = null,
) : AutoCloseable {
    fun adopt(staged: EnhancementPackageInstaller.Staged) {
        check(this.staged == null) { "Staged enhancement already owned" }
        this.staged = staged
    }

    fun transfer(): EnhancementPackageInstaller.Staged {
        return requireNotNull(staged).also { staged = null }
    }

    override fun close() {
        staged?.cleanup()
        staged = null
    }
}
