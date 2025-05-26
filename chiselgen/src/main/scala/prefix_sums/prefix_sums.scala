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
  val processSteps = new Counter(array_size / 2)
  val list_ind = RegInit(0.U(16.W))
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
        printf(cf"${loadCounter.value}\n")
        for (k <- 0 until lp) {
          listReg(k.U + list_ind) := io.inBlock.bits(k)
          loadCounter.inc()
          list_ind := list_ind + lp.U
        }
      }
      .otherwise {
        loadCounter.inc()
        when (loadCounter.value === ((array_size / lp)).U) {
          printf(cf"${loadCounter.value}\narr: ")
          for (i <- 0 until array_size) {
            printf(cf"${listReg(i)} ")
          }
          printf("\n")
          for (i <- 0 until array_size) {
            workTree((array_size - 1) + i) := listReg(i)
          }
          state := PrefixSumsState.process
        }
      }
    }
    is (PrefixSumsState.process) { // todo: finish
      printf(cf"${processSteps.value}\n")
      for (i <- 0 until 8 by 2) {
        workTree(i) := workTree(i + 7) + workTree(i + 8)
      }
      when (processSteps.value === (array_size / 2).U) {
        state := PrefixSumsState.done
      }
      processSteps.inc()
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
