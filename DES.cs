using Demo_ANM;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Demo_ANM // Quan trọng: Phải đúng tên này
{
    internal class DES
    {
        // --- CÁC BẢNG THAM SỐ (GIỮ NGUYÊN) ---
        // Ví dụ IP: Quy định bit thứ 58 của đầu vào sẽ chuyển sang vị trí số 1.
        private static readonly int[] PC1 = { 57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35, 27, 19, 11, 3, 60, 52, 44, 36, 63, 55, 47, 39, 31, 23, 15, 7, 62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5, 28, 20, 12, 4 };
        private static readonly int[] PC2 = { 14, 17, 11, 24, 1, 5, 3, 28, 15, 6, 21, 10, 23, 19, 12, 4, 26, 8, 16, 7, 27, 20, 13, 2, 41, 52, 31, 37, 47, 55, 30, 40, 51, 45, 33, 48, 44, 49, 39, 56, 34, 53, 46, 42, 50, 36, 29, 32 };
        private static readonly int[] E = { 32, 1, 2, 3, 4, 5, 4, 5, 6, 7, 8, 9, 8, 9, 10, 11, 12, 13, 12, 13, 14, 15, 16, 17, 16, 17, 18, 19, 20, 21, 20, 21, 22, 23, 24, 25, 24, 25, 26, 27, 28, 29, 28, 29, 30, 31, 32, 1 };
        private static readonly int[] P = { 16, 7, 20, 21, 29, 12, 28, 17, 1, 15, 23, 26, 5, 18, 31, 10, 2, 8, 24, 14, 32, 27, 3, 9, 19, 13, 30, 6, 22, 11, 4, 25 };
        private static readonly int[] IP = { 58, 50, 42, 34, 26, 18, 10, 2, 60, 52, 44, 36, 28, 20, 12, 4, 62, 54, 46, 38, 30, 22, 14, 6, 64, 56, 48, 40, 32, 24, 16, 8, 57, 49, 41, 33, 25, 17, 9, 1, 59, 51, 43, 35, 27, 19, 11, 3, 61, 53, 45, 37, 29, 21, 13, 5, 63, 55, 47, 39, 31, 23, 15, 7 };
        private static readonly int[] IP2 = { 40, 8, 48, 16, 56, 24, 64, 32, 39, 7, 47, 15, 55, 23, 63, 31, 38, 6, 46, 14, 54, 22, 62, 30, 37, 5, 45, 13, 53, 21, 61, 29, 36, 4, 44, 12, 52, 20, 60, 28, 35, 3, 43, 11, 51, 19, 59, 27, 34, 2, 42, 10, 50, 18, 58, 26, 33, 1, 41, 9, 49, 17, 57, 25 };

        // S-BOXES
        private static readonly int[] S1 = { 14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7, 0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8, 4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0, 15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13 };
        private static readonly int[] S2 = { 15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10, 3, 13, 4, 7, 15, 2, 8, 14, 12, 0, 1, 10, 6, 9, 11, 5, 0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15, 13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9 };
        private static readonly int[] S3 = { 10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8, 13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1, 13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7, 1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12 };
        private static readonly int[] S4 = { 7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15, 13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9, 10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4, 3, 15, 0, 6, 10, 1, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14 };
        private static readonly int[] S5 = { 2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9, 14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6, 4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14, 11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3 };
        private static readonly int[] S6 = { 12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11, 10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8, 9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6, 4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13 };
        private static readonly int[] S7 = { 4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1, 13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6, 1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2, 6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12 };
        private static readonly int[] S8 = { 13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7, 1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2, 7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8, 2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11 };
        // ==> Đây là các mảng static readonly. Nó là "luật chơi" của DES, không được phép sửa đổi.

        public byte[] Encrypt(byte[] textbytes, byte[] keybytes)
        {
            return Process(textbytes, keybytes, true);
        }

        public byte[] Decrypt(byte[] textbytes, byte[] keybytes)
        {
            return Process(textbytes, keybytes, false);
        }

        // Gom chung logic vào 1 hàm Process để code gọn hơn
        private byte[] Process(byte[] textbytes, byte[] keybytes, bool isEncrypt)
        {
            // ===== KIỂM TRA KHÓA DES (BẮT BUỘC ĐÚNG 8 KÝ TỰ) =====
            if (keybytes == null || keybytes.Length != 8)
            {
                throw new Exception("Khóa DES phải đúng 8 ký tự (64-bit). Vui lòng nhập đủ 8 ký tự.");
            }
            // ===================================================

            // Padding PKCS#7 (để xử lý độ dài bất kỳ)
            if (isEncrypt)
            {
                int padding = 8 - (textbytes.Length % 8);
                byte[] temp = new byte[textbytes.Length + padding];
                Array.Copy(textbytes, temp, textbytes.Length);
                for (int i = textbytes.Length; i < temp.Length; i++)
                    temp[i] = (byte)padding;
                textbytes = temp;
            }

            int blockcount = textbytes.Length / 8;
            byte[][] subkeys = generatesubkeys(keybytes);

            byte[] tmp = new byte[8];
            byte[] holderL = new byte[4];
            byte[] holderR = new byte[4];

            for (int blocknum = 0; blocknum < blockcount; blocknum++)
            {
                // Hoán vị khởi tạo IP
                Array.Copy(textbytes, blocknum * 8, tmp, 0, 8);
                tmp = UseTable(tmp, IP);
                Array.Copy(tmp, 0, textbytes, blocknum * 8, 8);

                // Tách L và R
                Array.Copy(textbytes, blocknum * 8, holderL, 0, 4);
                Array.Copy(textbytes, blocknum * 8 + 4, holderR, 0, 4);

                // 16 Vòng Feistel
                for (int stage = 1; stage <= 16; stage++)
                {
                    // Lấy subkey (Mã hóa: 0->15, Giải mã: 15->0)
                    byte[] currentSubKey = isEncrypt ? subkeys[stage - 1] : subkeys[16 - stage];

                    byte[] buffholder = holderR; // Lưu R cũ

                    // Hàm F
                    holderR = UseTable(holderR, E); // Mở rộng
                    holderR = Auxx.XORBytes(holderR, currentSubKey); // XOR khóa
                    holderR = Sbox(holderR); // S-Box
                    holderR = UseTable(holderR, P); // Hoán vị P

                    holderR = Auxx.XORBytes(holderR, holderL); // XOR với L cũ
                    holderL = buffholder; // L mới = R cũ
                }

                // Hoán vị nghịch đảo IP-1
                Array.Copy(holderR, 0, textbytes, blocknum * 8, 4);
                Array.Copy(holderL, 0, textbytes, blocknum * 8 + 4, 4);

                Array.Copy(textbytes, blocknum * 8, tmp, 0, 8);
                tmp = UseTable(tmp, IP2);
                Array.Copy(tmp, 0, textbytes, blocknum * 8, 8);
            }

            // Bỏ Padding khi giải mã
            if (!isEncrypt)
            {
                int paddingLength = textbytes[textbytes.Length - 1];
                if (paddingLength > 0 && paddingLength <= 8)
                {
                    byte[] unpadded = new byte[textbytes.Length - paddingLength];
                    Array.Copy(textbytes, unpadded, unpadded.Length);
                    return unpadded;
                }
            }

            return textbytes;
        }


        private byte[] UseTable(byte[] arr, int[] table)
        {
            return Auxx.selectBits(arr, table);
        }

        private byte[] Sbox(byte[] input)
        {
            byte[] output = new byte[4];
            for (int section = 0; section < 8; section++)
            {
                int row = (Auxx.getBitAt(input, section * 6) << 1) + Auxx.getBitAt(input, section * 6 + 5);
                int col = 0;
                for (int i = 1; i <= 4; i++) col += Auxx.getBitAt(input, section * 6 + i) << (4 - i);

                int val = GetSBoxValue(section + 1, row, col);

                // Ghi 4 bit vào output
                for (int i = 0; i < 4; i++)
                {
                    Auxx.setBitAt(output, section * 4 + i, (val >> (3 - i)) & 1);
                }
            }
            return output;
        }

        private int GetSBoxValue(int sboxIndex, int row, int col)
        {
            switch (sboxIndex)
            {
                case 1: return S1[row * 16 + col];
                case 2: return S2[row * 16 + col];
                case 3: return S3[row * 16 + col];
                case 4: return S4[row * 16 + col];
                case 5: return S5[row * 16 + col];
                case 6: return S6[row * 16 + col];
                case 7: return S7[row * 16 + col];
                case 8: return S8[row * 16 + col];
                default: return 0;
            }
        }


        // Đầu vào: Khóa chính 64-bit (thực tế dùng 56 bit).
        // Xử lý:Đi qua PC1 => Tách đôi C và D.
        // Lặp 16 lần: Mỗi lần dịch trái C, D (dùng Auxx.rotateLeft) rồi ghép lại đi qua PC2.
        // Đầu ra: Mảng 16 khóa con ($K_1$ đến $K_{16}$), mỗi khóa 48 bit.
        public byte[][] generatesubkeys(byte[] keybytes)
        {
            byte[][] subkeyarray = new byte[16][];
            byte[] k56 = UseTable(keybytes, PC1);

            byte[] C = Auxx.selectBits(k56, 0, 28);
            byte[] D = Auxx.selectBits(k56, 28, 28);

            int[] shifts = { 1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1 };

            for (int i = 0; i < 16; i++)
            {
                C = Auxx.rotateLeft(C, 28, shifts[i]);
                D = Auxx.rotateLeft(D, 28, shifts[i]);

                // Ghép C và D
                byte[] CD = new byte[7]; // 56 bit = 7 byte
                                         // Logic ghép bit hơi phức tạp ở đây nên ta làm thủ công
                                         // Đơn giản hóa: Tạo mảng 56 bit tạm
                                         // Trong thực tế đoạn code cũ Auxx.GlueKey khá rườm rà.
                                         // Ta dùng cách đơn giản: map lại bit.
                                         // Tuy nhiên để code chạy ngay, ta dùng lại logic ghép mảng bit

                // ... (Do logic GlueKey phức tạp, ta dùng trick ghép bit thủ công)
                byte[] combined = new byte[8]; // Dư giả chút
                for (int b = 0; b < 28; b++) Auxx.setBitAt(combined, b, Auxx.getBitAt(C, b));
                for (int b = 0; b < 28; b++) Auxx.setBitAt(combined, 28 + b, Auxx.getBitAt(D, b));

                subkeyarray[i] = UseTable(combined, PC2);
            }
            return subkeyarray;
        }
    }
}