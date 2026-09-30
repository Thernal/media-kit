# Large files

**Status:** open

`MediaFile` holds the whole file in memory, as Act2Act's `FileEntity` did. Images after the pipeline are a
few MB; a picked PDF or video can be far larger. A file-backed `MediaFile` (a path or URI plus a way to
open a stream) would let an upload stream instead of copying. Until then, set `maxFileSizeBytes` on the
file picker — it refuses oversized files before reading them.
