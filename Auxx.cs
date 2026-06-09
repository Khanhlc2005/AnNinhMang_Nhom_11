
//namespace Demo_ANM;
using System;
using System.Linq;
using System.Text;
namespace Demo_ANM // Quan trọng: Phải đúng tên này
{
    public static class Auxx
    {
        // Hàm chuyển chuỗi Hex sang mảng Byte (Dùng cho nút Giải mã)
        public static byte[] StringToByteArray(string hex)
        {
            // Loại bỏ khoảng trắng hoặc ký tự thừa nếu có
            hex = hex.Replace(" ", "").Trim();
            if (hex.Length % 2 != 0) throw new ArgumentException("Chuỗi Hex không hợp lệ (lẻ ký tự).");

            return Enumerable.Range(0, hex.Length)
                .Where(x => x % 2 == 0)
                .Select(x => Convert.ToByte(hex.Substring(x, 2), 16))
                .ToArray();
        }

        // Hàm chuyển mảng Byte sang chuỗi Hex (Dùng để hiển thị Bản mã)
        public static string ByteArrayToString(byte[] ba)
        {
            StringBuilder hex = new StringBuilder(ba.Length * 2);
            foreach (byte b in ba)
                hex.AppendFormat("{0:X2}", b); // X2 để viết hoa (A, B, C...)
            return hex.ToString();
        }
        // Nhiệm vụ: Thực hiện phép XOR giữa 2 mảng byte.Quy tắc: Giống nhau = 0, Khác nhau = 1.

        public static byte[] XORBytes(byte[] a, byte[] b)
        {
            byte[] outer = new byte[a.Length];
            for (int i = 0; i < a.Length; i++)
            {
                outer[i] = (byte)(a[i] ^ b[i]);
            }
            return outer;
        }

        public static byte[] selectBits(byte[] inner, int pos, int len)
        {
            int numOfBytes = (len - 1) / 8 + 1;
            byte[] outer = new byte[numOfBytes];
            for (int i = 0; i < len; i++)
            {
                int val = Auxx.getBitAt(inner, pos + i);
                Auxx.setBitAt(outer, i, val);
            }
            return outer;
        }

        public static byte[] selectBits(byte[] inner, int[] map) // Sửa lại kiểu dữ liệu map thành int[] cho khớp với DES.cs
        {
            int numOfBytes = (map.Length - 1) / 8 + 1;
            byte[] outer = new byte[numOfBytes];
            for (int i = 0; i < map.Length; i++)
            {
                int val = getBitAt(inner, map[i] - 1);
                setBitAt(outer, i, val);
            }
            return outer;
        }
        // Nhiệm vụ: Lấy  giá trị 0/1 tại một vị trí cụ thể trong mảng byte.
        public static int getBitAt(byte[] data, int poz)
        {
            int posByte = poz / 8;
            int posBit = poz % 8;
            byte valByte = data[posByte];
            int valInt = valByte >> (7 - posBit) & 1;
            return valInt;
        }
        // Nhiệm vụ: Gán giá trị 0/1 tại một vị trí cụ thể trong mảng byte.
        public static void setBitAt(byte[] data, int pos, int val)
        {
            byte oldByte = data[pos / 8];
            oldByte = (byte)(((0xFF7F >> (pos % 8)) & oldByte) & 0x00FF);
            byte newByte = (byte)((val << (7 - (pos % 8))) | oldByte);
            data[pos / 8] = newByte;
        }
        // Nhiệm vụ: Đẩy các bit sang trái, bit nào bị đẩy ra ngoài sẽ vòng lại xuống cuối.
        public static byte[] rotateLeft(byte[] inner, int len, int step)
        {
            byte[] outer = new byte[(len - 1) / 8 + 1];
            for (int i = 0; i < len; i++)
            {
                int val = getBitAt(inner, (i + step) % len);
                setBitAt(outer, i, val);
            }
            return outer;
        }
    }
}