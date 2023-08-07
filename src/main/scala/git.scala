package scalaboot

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

def clone(local: os.Path, remote: String, branch: Option[String] = None) =
  import libgit.all.*
  Zone { implicit z =>
    val repo = alloc[Ptr[git_repository]]()
    val opts = alloc[git_clone_options]()

    inline def doOrError(inline f: Int, inline prefix: String) =
      Err.assert(
        f == 0,
        prefix + ": " + fromCString((!git_error_last()).message)
      )

    git_libgit2_init()

    doOrError(
      git_clone_options_init(opts, 1.toUInt),
      "failed to init git clone options"
    )
    branch.foreach { br =>
      (!opts).checkout_branch = toCString(br)
    }

    doOrError(
      git_clone(repo, toCString(remote), toCString(local.toString), opts),
      "Failed to clone"
    )
  }
end clone
