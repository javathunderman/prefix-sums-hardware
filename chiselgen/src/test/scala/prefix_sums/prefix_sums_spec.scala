// See README.md for license details.

package prefix_sums

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._

class PrefixSumsSpec extends AnyFreeSpec with ChiselScalatestTester {

  "load and start calculation correctly" in {
    test(new PrefixSums(16, 8, 2)) { dut =>
      dut.io.inBlock.valid.poke(true.B)
      dut.clock.step()
      dut.io.inBlock.bits(0).poke(1.S)
      dut.io.inBlock.bits(1).poke(2.S)
      dut.io.state.expect(PrefixSumsState.load)
      dut.clock.step()
      dut.io.inBlock.bits(0).poke(3.S)
      dut.io.inBlock.bits(1).poke(4.S)
      dut.clock.step()
      dut.io.inBlock.bits(0).poke(5.S)
      dut.io.inBlock.bits(1).poke(6.S)
      dut.clock.step()
      dut.io.inBlock.bits(0).poke(7.S)
      dut.io.inBlock.bits(1).poke(8.S)
      dut.clock.step(2)
      dut.io.state.expect(PrefixSumsState.process)
      dut.clock.step(4)
      dut.io.state.expect(PrefixSumsState.done)

    }
  }
}
