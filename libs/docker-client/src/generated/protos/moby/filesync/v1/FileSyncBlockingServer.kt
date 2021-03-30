// Code generated by Wire protocol buffer compiler, do not edit.
// Source: moby.filesync.v1.FileSync in github.com/moby/buildkit/session/filesync/filesync.proto
package moby.filesync.v1

import com.squareup.wire.MessageSink
import com.squareup.wire.MessageSource
import com.squareup.wire.Service
import com.squareup.wire.WireRpc
import kotlin.Unit

public interface FileSyncBlockingServer : Service {
  @WireRpc(
    path = "/moby.filesync.v1.FileSync/DiffCopy",
    requestAdapter = "moby.filesync.v1.BytesMessage#ADAPTER",
    responseAdapter = "moby.filesync.v1.BytesMessage#ADAPTER"
  )
  public fun DiffCopy(request: MessageSource<BytesMessage>, response: MessageSink<BytesMessage>):
      Unit

  @WireRpc(
    path = "/moby.filesync.v1.FileSync/TarStream",
    requestAdapter = "moby.filesync.v1.BytesMessage#ADAPTER",
    responseAdapter = "moby.filesync.v1.BytesMessage#ADAPTER"
  )
  public fun TarStream(request: MessageSource<BytesMessage>, response: MessageSink<BytesMessage>):
      Unit
}
