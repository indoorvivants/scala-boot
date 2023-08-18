import scala.scalanative.unsafe.*

opaque type Memory = (String, () => Unit)
object Memory:
  extension (f: Memory)
    def deallocate() =
      f._2()

type Captured[D] = D
object Captured:
  def unsafe[D <: AnyRef: Tag](value: D): (Ptr[Captured[D]], Memory) =
    import scalanative.runtime.*

    val rawptr = libc.malloc(sizeof[Captured[D]])
    val mem = fromRawPtr[Captured[D]](rawptr)
    val deallocate: Memory =
      (
        value.toString(),
        () =>
          GCRoots.removeRoot(value.asInstanceOf[Object])
          libc.free(toRawPtr[Captured[D]](mem))
      )

    val originalAddress = Intrinsics.castObjectToRawPtr(value)

    Intrinsics.storeObject(rawptr, value)

    GCRoots.addRoot(value)

    (mem, deallocate)
  end unsafe

end Captured

object GCRoots:
  private val references = new java.util.IdentityHashMap[Object, Unit]
  def addRoot(o: Object): Unit = references.put(o, ())
  def removeRoot(o: Object): Unit = references.remove(o)
