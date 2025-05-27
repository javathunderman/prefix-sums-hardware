package prefix_sums

import chisel3._
import chisel3.util._
import os.list
import prefix_sums.PrefixSumsState.process
import prefix_sums.PrefixSumsState.load

object PrefixSumsState extends ChiselEnum {
  val idle, load, process, done = Value
}

class PrefixSums(width: Int, array_size: Int, lp: Int) extends Module {
  val io = IO(new Bundle {
    val inBlock = Flipped(Decoupled(Vec(lp, SInt(width.W))))
    val outBlock = Valid(Vec(array_size + 1, SInt(width.W)))
    val state = Output(PrefixSumsState())
  })
  val state = RegInit(PrefixSumsState.idle)
  val listReg = Reg(Vec(array_size, SInt(width.W)))
  val workTree = Reg(Vec((array_size * 2) - 1, SInt(width.W)))
  val loadCounter = new Counter(array_size)
  val list_ind = RegInit(0.U(16.W))
  val processSteps = new Counter((array_size * 2))
  val startReadIndex = RegInit((array_size - 1).U(16.W))
  val startWriteIndex = RegInit(((array_size / 2) - 1).U(16.W))
  val entries = new Counter((array_size / 2) + 1)
  val levelSize = RegInit((array_size / 2).U)
  for (i <- 0 until array_size + 1) {
    io.outBlock.bits(i) := 0.S
  } 
  io.outBlock.valid := false.B
  io.inBlock.ready := true.B

  switch (state) {
    is (PrefixSumsState.idle) {
      when (io.inBlock.fire) {
        state := PrefixSumsState.load
      }
    }
    is (PrefixSumsState.load) {
      when (loadCounter.value < ((array_size / lp)).U) {
        for (k <- 0 until lp) {
          listReg(k.U + list_ind) := io.inBlock.bits(k)
          loadCounter.inc()
          list_ind := list_ind + lp.U
        }
      }
      .otherwise {
        loadCounter.inc()
        when (loadCounter.value === ((array_size / lp)).U) {
          for (i <- 0 until array_size) {
            workTree((array_size - 1) + i) := listReg(i)
          }
          state := PrefixSumsState.process
        }
      }
    }
    is (PrefixSumsState.process) {
      workTree(startWriteIndex) := workTree(startReadIndex) + workTree(startReadIndex + 1.U)
      startWriteIndex := startWriteIndex + 1.U
      startReadIndex := startReadIndex + 2.U
      entries.inc()
      processSteps.inc()
      // printf(cf"startWriteIndex ${startWriteIndex} startreadIndex ${startReadIndex} levelSize ${levelSize} \n")
      when (entries.value === (levelSize - 1.U)) {
        levelSize := levelSize / 2.U
        startWriteIndex := ((startWriteIndex / 2.U)) - (levelSize / 2.U)
        startReadIndex := ((startReadIndex + 1.U) - (levelSize * 2.U)) / 2.U
        entries.reset()
      }
      when (processSteps.value === (array_size - 2).U) { // one less than array_size due to tree shape, another to account for counter cycle delay
        state := PrefixSumsState.done
      }
    }
    is (PrefixSumsState.done) {
      printf("workTree: ")
      for (i <- 0 until (array_size * 2) - 1) {
        printf(cf"${workTree(i)} ")
      }
      printf("\n")
    }
  }
  io.state := state
}
