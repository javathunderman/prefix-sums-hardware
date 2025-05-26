# CMSC858N Final

Prefix sums/scan as a hardware circuit that can be deployed to FPGAs.

To run, install Filament, Calyx, and the fud build tool. Then run `fud e --to cocotb-out alu-demo.fil -s cocotb.data input.json -s calyx.flags ' -d canonicalize'` to simulate the pipelined ALU (or any other file).

To run the scan demos (written in Calyx IR), run `fud e --to dat --through icarus-verilog -s verilog.data data.json scan.futil`. This takes the inputs in `data.json` and seeds them as memories for the Icarus Verilog circuit simulator.

## Chisel Implementation
Currently working on a parameterizable hardware generator in Chisel. 
You can run this with chiseltest. 