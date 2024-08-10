package com.absinthe.anywhere_.model

import com.absinthe.anywhere_.utils.AppUtils
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object ShizukuProcess {

  fun exec(cmd: String): String {

    val process = Shizuku.newProcess(arrayOf("sh"), null, null)
    val outputStream = DataOutputStream(process.outputStream)

    outputStream.apply {
      writeBytes("$cmd\n")
      flush()
      writeBytes("exit\n")
      flush()
    }

    val sb = StringBuilder()

    val runnable = Runnable {
      val inputStream = BufferedReader(InputStreamReader(process.inputStream))
      val buf = CharArray(1024)
      var len: Int = inputStream.read(buf)

      while (len > 0) {
        sb.appendRange(buf, 0, len)
        len = inputStream.read(buf)
      }
      inputStream.close()
    }
    val inputThread = Thread(runnable)
    inputThread.start()

    process.waitFor()
    inputThread.join()

    if (AppUtils.atLeastO()) {
      process.destroyForcibly()
    } else {
      process.destroy()
    }

    return sb.toString()
  }
}
