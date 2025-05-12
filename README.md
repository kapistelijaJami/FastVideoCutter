# FastVideoCutter
You can select a start time and an end time or duration, and FastVideoCutter will cut the video very fast without the need to re-encode the file. Output file video quality will remain exactly the same. Accuracy won't be exact, since it has to start and stop at I-frames, but is close enough for lot of use cases. You can also enter accurate mode which is used the same way, but will re-encode the video and is slower. You can also enter URLs to video files and M3U8 files, so only the part you need will be downloaded. There is also a merge mode, where you can merge multiple videos of same codec.

## Requirements:

**For running the binary:**
* [ffmpeg](https://ffmpeg.org/) installation. Ffmpeg.exe must either be in the same folder as this binary, or be in PATH environment variable.

**For building source code:**
* [Processes](https://github.com/kapistelijaJami/Processes)
* [Timer](https://github.com/kapistelijaJami/Timer)
