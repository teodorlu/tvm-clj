(ns tvm-clj.jna.stream
  (:require [tvm-clj.jna.base :refer [make-tvm-jna-fn
                                      device-type->int
                                      device-id->int
                                      ptr-ptr
                                      check-call]]
            [tech.resource :as resource]
            [tvm-clj.bindings.protocols :refer [->tvm] :as bindings-proto]
            [tech.datatype.jna :as dtype-jna]
            [tech.jna :as jna])
  (:import [com.sun.jna Native NativeLibrary Pointer Function Platform]
           [com.sun.jna.ptr PointerByReference IntByReference LongByReference]))


(declare ensure-stream->ptr)


(make-tvm-jna-fn TVMStreamCreate
                 "Create a stream"
                 Integer
                 [device_type device-type->int]
                 [device_id device-id->int]
                 [out ptr-ptr])


(make-tvm-jna-fn TVMStreamFree
                 "Free a stream"
                 Integer
                 [device_type device-type->int]
                 [device_id device-id->int]
                 [stream jna/ensure-ptr])


(make-tvm-jna-fn TVMSetStream
                 "Set current stream"
                 Integer
                 [device_type device-type->int]
                 [device_id device-id->int]
                 [stream ensure-stream->ptr])

(make-tvm-jna-fn TVMSynchronize
                 "Synchronize stream with host"
                 Integer
                 [device_type device-type->int]
                 [device_id device-id->int]
                 [stream ensure-stream->ptr])


(make-tvm-jna-fn TVMStreamStreamSynchronize
                 "Synchronize stream with stream"
                 Integer
                 [device_type device-type->int]
                 [device_id device-id->int]
                 [src ensure-stream->ptr]
                 [dst ensure-stream->ptr])


(defrecord StreamHandle [device-type ^long device-id tvm-hdl]
  bindings-proto/PToTVM
  (->tvm [item] item)
  jna/PToPtr
  (->ptr-backing-store [item] tvm-hdl)
  bindings-proto/PTVMDeviceId
  (device-id [item] device-id)
  bindings-proto/PTVMDeviceType
  (device-type [item] device-type))


(defn ensure-stream->ptr
  [item]
  (let [item (->tvm item)]
    (jna/ensure-type StreamHandle item)
    (jna/->ptr-backing-store item)))


(defn create-stream
  ^StreamHandle [device-type ^long device-id]
  (let [retval (PointerByReference.)]
    (check-call (TVMStreamCreate device-type device-id retval))
    (resource/track (->StreamHandle device-type device-id (.getValue retval))
                    #(TVMStreamFree (.getValue retval))
                    [:gc :stack])))


(defn sync-stream-with-host
  [stream]
  (let [stream (->tvm stream)]
    (check-call (TVMSynchronize stream stream stream))))


(defn sync-stream-with-stream
  [stream]
  (let [stream (->tvm stream)]
    (check-call (TVMStreamStreamSynchronize stream stream stream))))


(defn set-current-thread-stream
  [stream]
  (let [stream (->tvm stream)]
    (check-call (TVMSetStream stream stream stream))))
