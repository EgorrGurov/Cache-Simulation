lui s5, 4096
addi a5, a5, 1024
sb a0, 0, a5
bne a5, s5, -8
lui s1, 0
lui t3, 15360
sb a0, 0, a5
addi s1, s1, 1024
bne s1, t3, -8
sb a5, 2048, a5
bne s1, s5,  32
add a7, a4, a5
mulh a1, a2, a3
mulhsu a4, a5, a6
mulhu a7, s0, s1
xori a6, a7, 25
srli a6, a5, 2
srai a7, a6, 1
bne s1, s5,  32
fence io, rw
mulhu a7, s0, s1
remu a3, a4, a5
srli a0, a0, 1
rem a0, a1, a2
or a2, a3, a4
and a3, a4, a5
srl s2, s3, s4

