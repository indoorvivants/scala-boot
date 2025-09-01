package mxml

import _root_.scala.scalanative.unsafe.*
import _root_.scala.scalanative.unsigned.*
import _root_.scala.scalanative.libc.*
import _root_.scala.scalanative.*

object predef:
  private[mxml] trait _BindgenEnumCInt[T](using eq: T =:= CInt):
    given Tag[T] = Tag.Int.asInstanceOf[Tag[T]]
    extension (inline t: T)
     inline def value: CInt = eq.apply(t)
     inline def int: CInt = eq.apply(t).toInt
  private[mxml] trait _BindgenEnumCUnsignedInt[T](using eq: T =:= CUnsignedInt):
    given Tag[T] = Tag.UInt.asInstanceOf[Tag[T]]
    extension (inline t: T)
     inline def value: CUnsignedInt = eq.apply(t)
     inline def int: CInt = eq.apply(t).toInt
     inline def uint: CUnsignedInt = eq.apply(t)


object enumerations:
  import predef.*
  opaque type mxml_add_e = CUnsignedInt
  object mxml_add_e extends _BindgenEnumCUnsignedInt[mxml_add_e]:
    given _tag: Tag[mxml_add_e] = Tag.UInt
    inline def define(inline a: Long): mxml_add_e = a.toUInt
    val MXML_ADD_BEFORE = define(0)
    val MXML_ADD_AFTER = define(1)
    inline def getName(inline value: mxml_add_e): Option[String] =
      inline value match
        case MXML_ADD_BEFORE => Some("MXML_ADD_BEFORE")
        case MXML_ADD_AFTER => Some("MXML_ADD_AFTER")
        case _ => _root_.scala.None
    extension (a: mxml_add_e)
      inline def &(b: mxml_add_e): mxml_add_e = a & b
      inline def |(b: mxml_add_e): mxml_add_e = a | b
      inline def is(b: mxml_add_e): Boolean = (a & b) == b

  opaque type mxml_descend_e = CInt
  object mxml_descend_e extends _BindgenEnumCInt[mxml_descend_e]:
    given _tag: Tag[mxml_descend_e] = Tag.Int
    inline def define(inline a: CInt): mxml_descend_e = a
    val MXML_DESCEND_FIRST = define(-1)
    val MXML_DESCEND_NONE = define(0)
    val MXML_DESCEND_ALL = define(1)
    inline def getName(inline value: mxml_descend_e): Option[String] =
      inline value match
        case MXML_DESCEND_FIRST => Some("MXML_DESCEND_FIRST")
        case MXML_DESCEND_NONE => Some("MXML_DESCEND_NONE")
        case MXML_DESCEND_ALL => Some("MXML_DESCEND_ALL")
        case _ => _root_.scala.None
    extension (a: mxml_descend_e)
      inline def &(b: mxml_descend_e): mxml_descend_e = a & b
      inline def |(b: mxml_descend_e): mxml_descend_e = a | b
      inline def is(b: mxml_descend_e): Boolean = (a & b) == b

  opaque type mxml_sax_event_e = CUnsignedInt
  object mxml_sax_event_e extends _BindgenEnumCUnsignedInt[mxml_sax_event_e]:
    given _tag: Tag[mxml_sax_event_e] = Tag.UInt
    inline def define(inline a: Long): mxml_sax_event_e = a.toUInt
    val MXML_SAX_EVENT_CDATA = define(0)
    val MXML_SAX_EVENT_COMMENT = define(1)
    val MXML_SAX_EVENT_DATA = define(2)
    val MXML_SAX_EVENT_DECLARATION = define(3)
    val MXML_SAX_EVENT_DIRECTIVE = define(4)
    val MXML_SAX_EVENT_ELEMENT_CLOSE = define(5)
    val MXML_SAX_EVENT_ELEMENT_OPEN = define(6)
    inline def getName(inline value: mxml_sax_event_e): Option[String] =
      inline value match
        case MXML_SAX_EVENT_CDATA => Some("MXML_SAX_EVENT_CDATA")
        case MXML_SAX_EVENT_COMMENT => Some("MXML_SAX_EVENT_COMMENT")
        case MXML_SAX_EVENT_DATA => Some("MXML_SAX_EVENT_DATA")
        case MXML_SAX_EVENT_DECLARATION => Some("MXML_SAX_EVENT_DECLARATION")
        case MXML_SAX_EVENT_DIRECTIVE => Some("MXML_SAX_EVENT_DIRECTIVE")
        case MXML_SAX_EVENT_ELEMENT_CLOSE => Some("MXML_SAX_EVENT_ELEMENT_CLOSE")
        case MXML_SAX_EVENT_ELEMENT_OPEN => Some("MXML_SAX_EVENT_ELEMENT_OPEN")
        case _ => _root_.scala.None
    extension (a: mxml_sax_event_e)
      inline def &(b: mxml_sax_event_e): mxml_sax_event_e = a & b
      inline def |(b: mxml_sax_event_e): mxml_sax_event_e = a | b
      inline def is(b: mxml_sax_event_e): Boolean = (a & b) == b

  opaque type mxml_type_e = CInt
  object mxml_type_e extends _BindgenEnumCInt[mxml_type_e]:
    given _tag: Tag[mxml_type_e] = Tag.Int
    inline def define(inline a: CInt): mxml_type_e = a
    val MXML_TYPE_IGNORE = define(-1)
    val MXML_TYPE_CDATA = define(0)
    val MXML_TYPE_COMMENT = define(1)
    val MXML_TYPE_DECLARATION = define(2)
    val MXML_TYPE_DIRECTIVE = define(3)
    val MXML_TYPE_ELEMENT = define(4)
    val MXML_TYPE_INTEGER = define(5)
    val MXML_TYPE_OPAQUE = define(6)
    val MXML_TYPE_REAL = define(7)
    val MXML_TYPE_TEXT = define(8)
    val MXML_TYPE_CUSTOM = define(9)
    inline def getName(inline value: mxml_type_e): Option[String] =
      inline value match
        case MXML_TYPE_IGNORE => Some("MXML_TYPE_IGNORE")
        case MXML_TYPE_CDATA => Some("MXML_TYPE_CDATA")
        case MXML_TYPE_COMMENT => Some("MXML_TYPE_COMMENT")
        case MXML_TYPE_DECLARATION => Some("MXML_TYPE_DECLARATION")
        case MXML_TYPE_DIRECTIVE => Some("MXML_TYPE_DIRECTIVE")
        case MXML_TYPE_ELEMENT => Some("MXML_TYPE_ELEMENT")
        case MXML_TYPE_INTEGER => Some("MXML_TYPE_INTEGER")
        case MXML_TYPE_OPAQUE => Some("MXML_TYPE_OPAQUE")
        case MXML_TYPE_REAL => Some("MXML_TYPE_REAL")
        case MXML_TYPE_TEXT => Some("MXML_TYPE_TEXT")
        case MXML_TYPE_CUSTOM => Some("MXML_TYPE_CUSTOM")
        case _ => _root_.scala.None
    extension (a: mxml_type_e)
      inline def &(b: mxml_type_e): mxml_type_e = a & b
      inline def |(b: mxml_type_e): mxml_type_e = a | b
      inline def is(b: mxml_type_e): Boolean = (a & b) == b

  opaque type mxml_ws_e = CUnsignedInt
  object mxml_ws_e extends _BindgenEnumCUnsignedInt[mxml_ws_e]:
    given _tag: Tag[mxml_ws_e] = Tag.UInt
    inline def define(inline a: Long): mxml_ws_e = a.toUInt
    val MXML_WS_BEFORE_OPEN = define(0)
    val MXML_WS_AFTER_OPEN = define(1)
    val MXML_WS_BEFORE_CLOSE = define(2)
    val MXML_WS_AFTER_CLOSE = define(3)
    inline def getName(inline value: mxml_ws_e): Option[String] =
      inline value match
        case MXML_WS_BEFORE_OPEN => Some("MXML_WS_BEFORE_OPEN")
        case MXML_WS_AFTER_OPEN => Some("MXML_WS_AFTER_OPEN")
        case MXML_WS_BEFORE_CLOSE => Some("MXML_WS_BEFORE_CLOSE")
        case MXML_WS_AFTER_CLOSE => Some("MXML_WS_AFTER_CLOSE")
        case _ => _root_.scala.None
    extension (a: mxml_ws_e)
      inline def &(b: mxml_ws_e): mxml_ws_e = a & b
      inline def |(b: mxml_ws_e): mxml_ws_e = a | b
      inline def is(b: mxml_ws_e): Boolean = (a & b) == b

object aliases:
  import _root_.mxml.enumerations.*
  import _root_.mxml.predef.*
  import _root_.mxml.aliases.*
  import _root_.mxml.structs.*
  type FILE = libc.stdio.FILE
  object FILE: 
    val _tag: Tag[FILE] = summon[Tag[libc.stdio.FILE]]
    inline def apply(inline o: libc.stdio.FILE): FILE = o
    extension (v: FILE)
      inline def value: libc.stdio.FILE = v

  type mxml_add_t = mxml_add_e
  object mxml_add_t: 
    given _tag: Tag[mxml_add_t] = mxml_add_e._tag
    inline def apply(inline o: mxml_add_e): mxml_add_t = o
    extension (v: mxml_add_t)
      inline def value: mxml_add_e = v

  opaque type mxml_custfree_cb_t = CFuncPtr2[Ptr[Byte], Ptr[Byte], Unit]
  object mxml_custfree_cb_t: 
    given _tag: Tag[mxml_custfree_cb_t] = Tag.materializeCFuncPtr2[Ptr[Byte], Ptr[Byte], Unit]
    inline def fromPtr(ptr: Ptr[Byte] | CVoidPtr): mxml_custfree_cb_t = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr2[Ptr[Byte], Ptr[Byte], Unit]): mxml_custfree_cb_t = o
    extension (v: mxml_custfree_cb_t)
      inline def value: CFuncPtr2[Ptr[Byte], Ptr[Byte], Unit] = v
      inline def toPtr: CVoidPtr = CFuncPtr.toPtr(v)

  opaque type mxml_custload_cb_t = CFuncPtr3[Ptr[Byte], Ptr[mxml_node_t], CString, Boolean]
  object mxml_custload_cb_t: 
    given _tag: Tag[mxml_custload_cb_t] = Tag.materializeCFuncPtr3[Ptr[Byte], Ptr[mxml_node_t], CString, Boolean]
    inline def fromPtr(ptr: Ptr[Byte] | CVoidPtr): mxml_custload_cb_t = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr3[Ptr[Byte], Ptr[mxml_node_t], CString, Boolean]): mxml_custload_cb_t = o
    extension (v: mxml_custload_cb_t)
      inline def value: CFuncPtr3[Ptr[Byte], Ptr[mxml_node_t], CString, Boolean] = v
      inline def toPtr: CVoidPtr = CFuncPtr.toPtr(v)

  opaque type mxml_custsave_cb_t = CFuncPtr2[Ptr[Byte], Ptr[mxml_node_t], CString]
  object mxml_custsave_cb_t: 
    given _tag: Tag[mxml_custsave_cb_t] = Tag.materializeCFuncPtr2[Ptr[Byte], Ptr[mxml_node_t], CString]
    inline def fromPtr(ptr: Ptr[Byte] | CVoidPtr): mxml_custsave_cb_t = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr2[Ptr[Byte], Ptr[mxml_node_t], CString]): mxml_custsave_cb_t = o
    extension (v: mxml_custsave_cb_t)
      inline def value: CFuncPtr2[Ptr[Byte], Ptr[mxml_node_t], CString] = v
      inline def toPtr: CVoidPtr = CFuncPtr.toPtr(v)

  type mxml_descend_t = mxml_descend_e
  object mxml_descend_t: 
    given _tag: Tag[mxml_descend_t] = mxml_descend_e._tag
    inline def apply(inline o: mxml_descend_e): mxml_descend_t = o
    extension (v: mxml_descend_t)
      inline def value: mxml_descend_e = v

  opaque type mxml_entity_cb_t = CFuncPtr2[Ptr[Byte], CString, CInt]
  object mxml_entity_cb_t: 
    given _tag: Tag[mxml_entity_cb_t] = Tag.materializeCFuncPtr2[Ptr[Byte], CString, CInt]
    inline def fromPtr(ptr: Ptr[Byte] | CVoidPtr): mxml_entity_cb_t = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr2[Ptr[Byte], CString, CInt]): mxml_entity_cb_t = o
    extension (v: mxml_entity_cb_t)
      inline def value: CFuncPtr2[Ptr[Byte], CString, CInt] = v
      inline def toPtr: CVoidPtr = CFuncPtr.toPtr(v)

  opaque type mxml_error_cb_t = CFuncPtr2[Ptr[Byte], CString, Unit]
  object mxml_error_cb_t: 
    given _tag: Tag[mxml_error_cb_t] = Tag.materializeCFuncPtr2[Ptr[Byte], CString, Unit]
    inline def fromPtr(ptr: Ptr[Byte] | CVoidPtr): mxml_error_cb_t = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr2[Ptr[Byte], CString, Unit]): mxml_error_cb_t = o
    extension (v: mxml_error_cb_t)
      inline def value: CFuncPtr2[Ptr[Byte], CString, Unit] = v
      inline def toPtr: CVoidPtr = CFuncPtr.toPtr(v)

  opaque type mxml_io_cb_t = CFuncPtr3[Ptr[Byte], Ptr[Byte], size_t, size_t]
  object mxml_io_cb_t: 
    given _tag: Tag[mxml_io_cb_t] = Tag.materializeCFuncPtr3[Ptr[Byte], Ptr[Byte], size_t, size_t]
    inline def fromPtr(ptr: Ptr[Byte] | CVoidPtr): mxml_io_cb_t = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr3[Ptr[Byte], Ptr[Byte], size_t, size_t]): mxml_io_cb_t = o
    extension (v: mxml_io_cb_t)
      inline def value: CFuncPtr3[Ptr[Byte], Ptr[Byte], size_t, size_t] = v
      inline def toPtr: CVoidPtr = CFuncPtr.toPtr(v)

  opaque type mxml_sax_cb_t = CFuncPtr3[Ptr[Byte], Ptr[mxml_node_t], mxml_sax_event_t, Boolean]
  object mxml_sax_cb_t: 
    given _tag: Tag[mxml_sax_cb_t] = Tag.materializeCFuncPtr3[Ptr[Byte], Ptr[mxml_node_t], mxml_sax_event_t, Boolean]
    inline def fromPtr(ptr: Ptr[Byte] | CVoidPtr): mxml_sax_cb_t = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr3[Ptr[Byte], Ptr[mxml_node_t], mxml_sax_event_t, Boolean]): mxml_sax_cb_t = o
    extension (v: mxml_sax_cb_t)
      inline def value: CFuncPtr3[Ptr[Byte], Ptr[mxml_node_t], mxml_sax_event_t, Boolean] = v
      inline def toPtr: CVoidPtr = CFuncPtr.toPtr(v)

  type mxml_sax_event_t = mxml_sax_event_e
  object mxml_sax_event_t: 
    given _tag: Tag[mxml_sax_event_t] = mxml_sax_event_e._tag
    inline def apply(inline o: mxml_sax_event_e): mxml_sax_event_t = o
    extension (v: mxml_sax_event_t)
      inline def value: mxml_sax_event_e = v

  opaque type mxml_strcopy_cb_t = CFuncPtr2[Ptr[Byte], CString, CString]
  object mxml_strcopy_cb_t: 
    given _tag: Tag[mxml_strcopy_cb_t] = Tag.materializeCFuncPtr2[Ptr[Byte], CString, CString]
    inline def fromPtr(ptr: Ptr[Byte] | CVoidPtr): mxml_strcopy_cb_t = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr2[Ptr[Byte], CString, CString]): mxml_strcopy_cb_t = o
    extension (v: mxml_strcopy_cb_t)
      inline def value: CFuncPtr2[Ptr[Byte], CString, CString] = v
      inline def toPtr: CVoidPtr = CFuncPtr.toPtr(v)

  opaque type mxml_strfree_cb_t = CFuncPtr2[Ptr[Byte], CString, Unit]
  object mxml_strfree_cb_t: 
    given _tag: Tag[mxml_strfree_cb_t] = Tag.materializeCFuncPtr2[Ptr[Byte], CString, Unit]
    inline def fromPtr(ptr: Ptr[Byte] | CVoidPtr): mxml_strfree_cb_t = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr2[Ptr[Byte], CString, Unit]): mxml_strfree_cb_t = o
    extension (v: mxml_strfree_cb_t)
      inline def value: CFuncPtr2[Ptr[Byte], CString, Unit] = v
      inline def toPtr: CVoidPtr = CFuncPtr.toPtr(v)

  opaque type mxml_type_cb_t = CFuncPtr2[Ptr[Byte], Ptr[mxml_node_t], mxml_type_t]
  object mxml_type_cb_t: 
    given _tag: Tag[mxml_type_cb_t] = Tag.materializeCFuncPtr2[Ptr[Byte], Ptr[mxml_node_t], mxml_type_t]
    inline def fromPtr(ptr: Ptr[Byte] | CVoidPtr): mxml_type_cb_t = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr2[Ptr[Byte], Ptr[mxml_node_t], mxml_type_t]): mxml_type_cb_t = o
    extension (v: mxml_type_cb_t)
      inline def value: CFuncPtr2[Ptr[Byte], Ptr[mxml_node_t], mxml_type_t] = v
      inline def toPtr: CVoidPtr = CFuncPtr.toPtr(v)

  type mxml_type_t = mxml_type_e
  object mxml_type_t: 
    given _tag: Tag[mxml_type_t] = mxml_type_e._tag
    inline def apply(inline o: mxml_type_e): mxml_type_t = o
    extension (v: mxml_type_t)
      inline def value: mxml_type_e = v

  opaque type mxml_ws_cb_t = CFuncPtr3[Ptr[Byte], Ptr[mxml_node_t], mxml_ws_t, CString]
  object mxml_ws_cb_t: 
    given _tag: Tag[mxml_ws_cb_t] = Tag.materializeCFuncPtr3[Ptr[Byte], Ptr[mxml_node_t], mxml_ws_t, CString]
    inline def fromPtr(ptr: Ptr[Byte] | CVoidPtr): mxml_ws_cb_t = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr3[Ptr[Byte], Ptr[mxml_node_t], mxml_ws_t, CString]): mxml_ws_cb_t = o
    extension (v: mxml_ws_cb_t)
      inline def value: CFuncPtr3[Ptr[Byte], Ptr[mxml_node_t], mxml_ws_t, CString] = v
      inline def toPtr: CVoidPtr = CFuncPtr.toPtr(v)

  type mxml_ws_t = mxml_ws_e
  object mxml_ws_t: 
    given _tag: Tag[mxml_ws_t] = mxml_ws_e._tag
    inline def apply(inline o: mxml_ws_e): mxml_ws_t = o
    extension (v: mxml_ws_t)
      inline def value: mxml_ws_e = v

  type size_t = libc.stddef.size_t
  object size_t: 
    val _tag: Tag[size_t] = summon[Tag[libc.stddef.size_t]]
    inline def apply(inline o: libc.stddef.size_t): size_t = o
    extension (v: size_t)
      inline def value: libc.stddef.size_t = v

object structs:
  import _root_.mxml.enumerations.*
  import _root_.mxml.predef.*
  import _root_.mxml.aliases.*
  import _root_.mxml.structs.*
  opaque type _mxml_index_s = CStruct0
  object _mxml_index_s:
    given _tag: Tag[_mxml_index_s] = Tag.materializeCStruct0Tag

  opaque type _mxml_node_s = CStruct0
  object _mxml_node_s:
    given _tag: Tag[_mxml_node_s] = Tag.materializeCStruct0Tag

  opaque type _mxml_options_s = CStruct0
  object _mxml_options_s:
    given _tag: Tag[_mxml_options_s] = Tag.materializeCStruct0Tag

  opaque type mxml_index_t = CStruct0
  object mxml_index_t:
    given _tag: Tag[mxml_index_t] = Tag.materializeCStruct0Tag

  opaque type mxml_node_t = CStruct0
  object mxml_node_t:
    given _tag: Tag[mxml_node_t] = Tag.materializeCStruct0Tag

  opaque type mxml_options_t = CStruct0
  object mxml_options_t:
    given _tag: Tag[mxml_options_t] = Tag.materializeCStruct0Tag


@extern
private[mxml] object extern_functions:
  import _root_.mxml.enumerations.*
  import _root_.mxml.predef.*
  import _root_.mxml.aliases.*
  import _root_.mxml.structs.*
  def mxmlAdd(parent : Ptr[mxml_node_t], add : mxml_add_t, child : Ptr[mxml_node_t], node : Ptr[mxml_node_t]): Unit = extern

  def mxmlDelete(node : Ptr[mxml_node_t]): Unit = extern

  def mxmlElementClearAttr(node : Ptr[mxml_node_t], name : CString): Unit = extern

  def mxmlElementGetAttr(node : Ptr[mxml_node_t], name : CString): CString = extern

  def mxmlElementGetAttrByIndex(node : Ptr[mxml_node_t], idx : size_t, name : Ptr[CString]): CString = extern

  def mxmlElementGetAttrCount(node : Ptr[mxml_node_t]): size_t = extern

  def mxmlElementSetAttr(node : Ptr[mxml_node_t], name : CString, value : CString): Unit = extern

  def mxmlElementSetAttrf(node : Ptr[mxml_node_t], name : CString, format : CString, rest: Any*): Unit = extern

  def mxmlFindElement(node : Ptr[mxml_node_t], top : Ptr[mxml_node_t], element : CString, attr : CString, value : CString, descend : mxml_descend_t): Ptr[mxml_node_t] = extern

  def mxmlFindPath(node : Ptr[mxml_node_t], path : CString): Ptr[mxml_node_t] = extern

  def mxmlGetCDATA(node : Ptr[mxml_node_t]): CString = extern

  def mxmlGetComment(node : Ptr[mxml_node_t]): CString = extern

  def mxmlGetCustom(node : Ptr[mxml_node_t]): Ptr[Byte] = extern

  def mxmlGetDeclaration(node : Ptr[mxml_node_t]): CString = extern

  def mxmlGetDirective(node : Ptr[mxml_node_t]): CString = extern

  def mxmlGetElement(node : Ptr[mxml_node_t]): CString = extern

  def mxmlGetFirstChild(node : Ptr[mxml_node_t]): Ptr[mxml_node_t] = extern

  def mxmlGetInteger(node : Ptr[mxml_node_t]): CLongInt = extern

  def mxmlGetLastChild(node : Ptr[mxml_node_t]): Ptr[mxml_node_t] = extern

  def mxmlGetNextSibling(node : Ptr[mxml_node_t]): Ptr[mxml_node_t] = extern

  def mxmlGetOpaque(node : Ptr[mxml_node_t]): CString = extern

  def mxmlGetParent(node : Ptr[mxml_node_t]): Ptr[mxml_node_t] = extern

  def mxmlGetPrevSibling(node : Ptr[mxml_node_t]): Ptr[mxml_node_t] = extern

  def mxmlGetReal(node : Ptr[mxml_node_t]): Double = extern

  def mxmlGetRefCount(node : Ptr[mxml_node_t]): size_t = extern

  def mxmlGetText(node : Ptr[mxml_node_t], whitespace : Ptr[Boolean]): CString = extern

  def mxmlGetType(node : Ptr[mxml_node_t]): mxml_type_t = extern

  def mxmlGetUserData(node : Ptr[mxml_node_t]): Ptr[Byte] = extern

  def mxmlIndexDelete(ind : Ptr[mxml_index_t]): Unit = extern

  def mxmlIndexEnum(ind : Ptr[mxml_index_t]): Ptr[mxml_node_t] = extern

  def mxmlIndexFind(ind : Ptr[mxml_index_t], element : CString, value : CString): Ptr[mxml_node_t] = extern

  def mxmlIndexGetCount(ind : Ptr[mxml_index_t]): size_t = extern

  def mxmlIndexNew(node : Ptr[mxml_node_t], element : CString, attr : CString): Ptr[mxml_index_t] = extern

  def mxmlIndexReset(ind : Ptr[mxml_index_t]): Ptr[mxml_node_t] = extern

  def mxmlLoadFd(top : Ptr[mxml_node_t], options : Ptr[mxml_options_t], fd : CInt): Ptr[mxml_node_t] = extern

  def mxmlLoadFile(top : Ptr[mxml_node_t], options : Ptr[mxml_options_t], fp : Ptr[FILE]): Ptr[mxml_node_t] = extern

  def mxmlLoadFilename(top : Ptr[mxml_node_t], options : Ptr[mxml_options_t], filename : CString): Ptr[mxml_node_t] = extern

  def mxmlLoadIO(top : Ptr[mxml_node_t], options : Ptr[mxml_options_t], io_cb : mxml_io_cb_t, io_cbdata : Ptr[Byte]): Ptr[mxml_node_t] = extern

  def mxmlLoadString(top : Ptr[mxml_node_t], options : Ptr[mxml_options_t], s : CString): Ptr[mxml_node_t] = extern

  def mxmlNewCDATA(parent : Ptr[mxml_node_t], string : CString): Ptr[mxml_node_t] = extern

  def mxmlNewCDATAf(parent : Ptr[mxml_node_t], format : CString, rest: Any*): Ptr[mxml_node_t] = extern

  def mxmlNewComment(parent : Ptr[mxml_node_t], comment : CString): Ptr[mxml_node_t] = extern

  def mxmlNewCommentf(parent : Ptr[mxml_node_t], format : CString, rest: Any*): Ptr[mxml_node_t] = extern

  def mxmlNewCustom(parent : Ptr[mxml_node_t], data : Ptr[Byte], free_cb : mxml_custfree_cb_t, free_cbdata : Ptr[Byte]): Ptr[mxml_node_t] = extern

  def mxmlNewDeclaration(parent : Ptr[mxml_node_t], declaration : CString): Ptr[mxml_node_t] = extern

  def mxmlNewDeclarationf(parent : Ptr[mxml_node_t], format : CString, rest: Any*): Ptr[mxml_node_t] = extern

  def mxmlNewDirective(parent : Ptr[mxml_node_t], directive : CString): Ptr[mxml_node_t] = extern

  def mxmlNewDirectivef(parent : Ptr[mxml_node_t], format : CString, rest: Any*): Ptr[mxml_node_t] = extern

  def mxmlNewElement(parent : Ptr[mxml_node_t], name : CString): Ptr[mxml_node_t] = extern

  def mxmlNewInteger(parent : Ptr[mxml_node_t], integer : CLongInt): Ptr[mxml_node_t] = extern

  def mxmlNewOpaque(parent : Ptr[mxml_node_t], opaque : CString): Ptr[mxml_node_t] = extern

  def mxmlNewOpaquef(parent : Ptr[mxml_node_t], format : CString, rest: Any*): Ptr[mxml_node_t] = extern

  def mxmlNewReal(parent : Ptr[mxml_node_t], real : Double): Ptr[mxml_node_t] = extern

  def mxmlNewText(parent : Ptr[mxml_node_t], whitespace : Boolean, string : CString): Ptr[mxml_node_t] = extern

  def mxmlNewTextf(parent : Ptr[mxml_node_t], whitespace : Boolean, format : CString, rest: Any*): Ptr[mxml_node_t] = extern

  def mxmlNewXML(version : CString): Ptr[mxml_node_t] = extern

  def mxmlOptionsDelete(options : Ptr[mxml_options_t]): Unit = extern

  def mxmlOptionsNew(): Ptr[mxml_options_t] = extern

  def mxmlOptionsSetCustomCallbacks(options : Ptr[mxml_options_t], load_cb : mxml_custload_cb_t, save_cb : mxml_custsave_cb_t, cbdata : Ptr[Byte]): Unit = extern

  def mxmlOptionsSetEntityCallback(options : Ptr[mxml_options_t], cb : mxml_entity_cb_t, cbdata : Ptr[Byte]): Unit = extern

  def mxmlOptionsSetErrorCallback(options : Ptr[mxml_options_t], cb : mxml_error_cb_t, cbdata : Ptr[Byte]): Unit = extern

  def mxmlOptionsSetSAXCallback(options : Ptr[mxml_options_t], cb : mxml_sax_cb_t, cbdata : Ptr[Byte]): Unit = extern

  def mxmlOptionsSetTypeCallback(options : Ptr[mxml_options_t], cb : mxml_type_cb_t, cbdata : Ptr[Byte]): Unit = extern

  def mxmlOptionsSetTypeValue(options : Ptr[mxml_options_t], `type` : mxml_type_t): Unit = extern

  def mxmlOptionsSetWhitespaceCallback(options : Ptr[mxml_options_t], cb : mxml_ws_cb_t, cbdata : Ptr[Byte]): Unit = extern

  def mxmlOptionsSetWrapMargin(options : Ptr[mxml_options_t], column : CInt): Unit = extern

  def mxmlRelease(node : Ptr[mxml_node_t]): CInt = extern

  def mxmlRemove(node : Ptr[mxml_node_t]): Unit = extern

  def mxmlRetain(node : Ptr[mxml_node_t]): CInt = extern

  def mxmlSaveAllocString(node : Ptr[mxml_node_t], options : Ptr[mxml_options_t]): CString = extern

  def mxmlSaveFd(node : Ptr[mxml_node_t], options : Ptr[mxml_options_t], fd : CInt): Boolean = extern

  def mxmlSaveFile(node : Ptr[mxml_node_t], options : Ptr[mxml_options_t], fp : Ptr[FILE]): Boolean = extern

  def mxmlSaveFilename(node : Ptr[mxml_node_t], options : Ptr[mxml_options_t], filename : CString): Boolean = extern

  def mxmlSaveIO(node : Ptr[mxml_node_t], options : Ptr[mxml_options_t], io_cb : mxml_io_cb_t, io_cbdata : Ptr[Byte]): Boolean = extern

  def mxmlSaveString(node : Ptr[mxml_node_t], options : Ptr[mxml_options_t], buffer : CString, bufsize : size_t): size_t = extern

  def mxmlSetCDATA(node : Ptr[mxml_node_t], data : CString): Boolean = extern

  def mxmlSetCDATAf(node : Ptr[mxml_node_t], format : CString, rest: Any*): Boolean = extern

  def mxmlSetComment(node : Ptr[mxml_node_t], comment : CString): Boolean = extern

  def mxmlSetCommentf(node : Ptr[mxml_node_t], format : CString, rest: Any*): Boolean = extern

  def mxmlSetCustom(node : Ptr[mxml_node_t], data : Ptr[Byte], free_cb : mxml_custfree_cb_t, free_cbdata : Ptr[Byte]): Boolean = extern

  def mxmlSetDeclaration(node : Ptr[mxml_node_t], declaration : CString): Boolean = extern

  def mxmlSetDeclarationf(node : Ptr[mxml_node_t], format : CString, rest: Any*): Boolean = extern

  def mxmlSetDirective(node : Ptr[mxml_node_t], directive : CString): Boolean = extern

  def mxmlSetDirectivef(node : Ptr[mxml_node_t], format : CString, rest: Any*): Boolean = extern

  def mxmlSetElement(node : Ptr[mxml_node_t], name : CString): Boolean = extern

  def mxmlSetInteger(node : Ptr[mxml_node_t], integer : CLongInt): Boolean = extern

  def mxmlSetOpaque(node : Ptr[mxml_node_t], opaque : CString): Boolean = extern

  def mxmlSetOpaquef(node : Ptr[mxml_node_t], format : CString, rest: Any*): Boolean = extern

  def mxmlSetReal(node : Ptr[mxml_node_t], real : Double): Boolean = extern

  def mxmlSetStringCallbacks(strcopy_cb : mxml_strcopy_cb_t, strfree_cb : mxml_strfree_cb_t, str_cbdata : Ptr[Byte]): Unit = extern

  def mxmlSetText(node : Ptr[mxml_node_t], whitespace : Boolean, string : CString): Boolean = extern

  def mxmlSetTextf(node : Ptr[mxml_node_t], whitespace : Boolean, format : CString, rest: Any*): Boolean = extern

  def mxmlSetUserData(node : Ptr[mxml_node_t], data : Ptr[Byte]): Boolean = extern

  def mxmlWalkNext(node : Ptr[mxml_node_t], top : Ptr[mxml_node_t], descend : mxml_descend_t): Ptr[mxml_node_t] = extern

  def mxmlWalkPrev(node : Ptr[mxml_node_t], top : Ptr[mxml_node_t], descend : mxml_descend_t): Ptr[mxml_node_t] = extern


object functions:
  import _root_.mxml.enumerations.*
  import _root_.mxml.predef.*
  import _root_.mxml.aliases.*
  import _root_.mxml.structs.*
  import extern_functions.*
  export extern_functions.*

object types:
  export _root_.mxml.structs.*
  export _root_.mxml.aliases.*
  export _root_.mxml.enumerations.*

object all:
  export _root_.mxml.enumerations.mxml_add_e
  export _root_.mxml.enumerations.mxml_descend_e
  export _root_.mxml.enumerations.mxml_sax_event_e
  export _root_.mxml.enumerations.mxml_type_e
  export _root_.mxml.enumerations.mxml_ws_e
  export _root_.mxml.aliases.FILE
  export _root_.mxml.aliases.mxml_add_t
  export _root_.mxml.aliases.mxml_custfree_cb_t
  export _root_.mxml.aliases.mxml_custload_cb_t
  export _root_.mxml.aliases.mxml_custsave_cb_t
  export _root_.mxml.aliases.mxml_descend_t
  export _root_.mxml.aliases.mxml_entity_cb_t
  export _root_.mxml.aliases.mxml_error_cb_t
  export _root_.mxml.aliases.mxml_io_cb_t
  export _root_.mxml.aliases.mxml_sax_cb_t
  export _root_.mxml.aliases.mxml_sax_event_t
  export _root_.mxml.aliases.mxml_strcopy_cb_t
  export _root_.mxml.aliases.mxml_strfree_cb_t
  export _root_.mxml.aliases.mxml_type_cb_t
  export _root_.mxml.aliases.mxml_type_t
  export _root_.mxml.aliases.mxml_ws_cb_t
  export _root_.mxml.aliases.mxml_ws_t
  export _root_.mxml.aliases.size_t
  export _root_.mxml.structs._mxml_index_s
  export _root_.mxml.structs._mxml_node_s
  export _root_.mxml.structs._mxml_options_s
  export _root_.mxml.structs.mxml_index_t
  export _root_.mxml.structs.mxml_node_t
  export _root_.mxml.structs.mxml_options_t
  export _root_.mxml.functions.mxmlAdd
  export _root_.mxml.functions.mxmlDelete
  export _root_.mxml.functions.mxmlElementClearAttr
  export _root_.mxml.functions.mxmlElementGetAttr
  export _root_.mxml.functions.mxmlElementGetAttrByIndex
  export _root_.mxml.functions.mxmlElementGetAttrCount
  export _root_.mxml.functions.mxmlElementSetAttr
  export _root_.mxml.functions.mxmlElementSetAttrf
  export _root_.mxml.functions.mxmlFindElement
  export _root_.mxml.functions.mxmlFindPath
  export _root_.mxml.functions.mxmlGetCDATA
  export _root_.mxml.functions.mxmlGetComment
  export _root_.mxml.functions.mxmlGetCustom
  export _root_.mxml.functions.mxmlGetDeclaration
  export _root_.mxml.functions.mxmlGetDirective
  export _root_.mxml.functions.mxmlGetElement
  export _root_.mxml.functions.mxmlGetFirstChild
  export _root_.mxml.functions.mxmlGetInteger
  export _root_.mxml.functions.mxmlGetLastChild
  export _root_.mxml.functions.mxmlGetNextSibling
  export _root_.mxml.functions.mxmlGetOpaque
  export _root_.mxml.functions.mxmlGetParent
  export _root_.mxml.functions.mxmlGetPrevSibling
  export _root_.mxml.functions.mxmlGetReal
  export _root_.mxml.functions.mxmlGetRefCount
  export _root_.mxml.functions.mxmlGetText
  export _root_.mxml.functions.mxmlGetType
  export _root_.mxml.functions.mxmlGetUserData
  export _root_.mxml.functions.mxmlIndexDelete
  export _root_.mxml.functions.mxmlIndexEnum
  export _root_.mxml.functions.mxmlIndexFind
  export _root_.mxml.functions.mxmlIndexGetCount
  export _root_.mxml.functions.mxmlIndexNew
  export _root_.mxml.functions.mxmlIndexReset
  export _root_.mxml.functions.mxmlLoadFd
  export _root_.mxml.functions.mxmlLoadFile
  export _root_.mxml.functions.mxmlLoadFilename
  export _root_.mxml.functions.mxmlLoadIO
  export _root_.mxml.functions.mxmlLoadString
  export _root_.mxml.functions.mxmlNewCDATA
  export _root_.mxml.functions.mxmlNewCDATAf
  export _root_.mxml.functions.mxmlNewComment
  export _root_.mxml.functions.mxmlNewCommentf
  export _root_.mxml.functions.mxmlNewCustom
  export _root_.mxml.functions.mxmlNewDeclaration
  export _root_.mxml.functions.mxmlNewDeclarationf
  export _root_.mxml.functions.mxmlNewDirective
  export _root_.mxml.functions.mxmlNewDirectivef
  export _root_.mxml.functions.mxmlNewElement
  export _root_.mxml.functions.mxmlNewInteger
  export _root_.mxml.functions.mxmlNewOpaque
  export _root_.mxml.functions.mxmlNewOpaquef
  export _root_.mxml.functions.mxmlNewReal
  export _root_.mxml.functions.mxmlNewText
  export _root_.mxml.functions.mxmlNewTextf
  export _root_.mxml.functions.mxmlNewXML
  export _root_.mxml.functions.mxmlOptionsDelete
  export _root_.mxml.functions.mxmlOptionsNew
  export _root_.mxml.functions.mxmlOptionsSetCustomCallbacks
  export _root_.mxml.functions.mxmlOptionsSetEntityCallback
  export _root_.mxml.functions.mxmlOptionsSetErrorCallback
  export _root_.mxml.functions.mxmlOptionsSetSAXCallback
  export _root_.mxml.functions.mxmlOptionsSetTypeCallback
  export _root_.mxml.functions.mxmlOptionsSetTypeValue
  export _root_.mxml.functions.mxmlOptionsSetWhitespaceCallback
  export _root_.mxml.functions.mxmlOptionsSetWrapMargin
  export _root_.mxml.functions.mxmlRelease
  export _root_.mxml.functions.mxmlRemove
  export _root_.mxml.functions.mxmlRetain
  export _root_.mxml.functions.mxmlSaveAllocString
  export _root_.mxml.functions.mxmlSaveFd
  export _root_.mxml.functions.mxmlSaveFile
  export _root_.mxml.functions.mxmlSaveFilename
  export _root_.mxml.functions.mxmlSaveIO
  export _root_.mxml.functions.mxmlSaveString
  export _root_.mxml.functions.mxmlSetCDATA
  export _root_.mxml.functions.mxmlSetCDATAf
  export _root_.mxml.functions.mxmlSetComment
  export _root_.mxml.functions.mxmlSetCommentf
  export _root_.mxml.functions.mxmlSetCustom
  export _root_.mxml.functions.mxmlSetDeclaration
  export _root_.mxml.functions.mxmlSetDeclarationf
  export _root_.mxml.functions.mxmlSetDirective
  export _root_.mxml.functions.mxmlSetDirectivef
  export _root_.mxml.functions.mxmlSetElement
  export _root_.mxml.functions.mxmlSetInteger
  export _root_.mxml.functions.mxmlSetOpaque
  export _root_.mxml.functions.mxmlSetOpaquef
  export _root_.mxml.functions.mxmlSetReal
  export _root_.mxml.functions.mxmlSetStringCallbacks
  export _root_.mxml.functions.mxmlSetText
  export _root_.mxml.functions.mxmlSetTextf
  export _root_.mxml.functions.mxmlSetUserData
  export _root_.mxml.functions.mxmlWalkNext
  export _root_.mxml.functions.mxmlWalkPrev