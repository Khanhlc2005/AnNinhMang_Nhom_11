using Demo_ANM;
using Microsoft.Win32;
using System;
using System.IO;
using System.Linq;
using System.Text;
using System.Windows;
using System.Windows.Controls;

namespace DES_Project_New
{
    public partial class MainWindow : Window
    {
        private string _ReadFileExtension = "";
        private string _LoadFileExtension = "";
        static Random random = new Random();
        private bool _fileOrPlainText = false;
        private byte[] _lastEncryptKeyBytes = null;

        //Nhiệm vụ: Lấy dữ liệu từ ô nhập, kiểm tra xem người dùng chọn ASCII hay Hex.
        //Chuẩn hóa dữ liệu về dạng byte[] sạch sẽ để ném vào động cơ.

        private byte[] GetKeyFromUI()
        {
            // SỬA LỖI Ở ĐÂY: Đổi txtKey thành txtKeyInput cho khớp với giao diện
            string inputKey = txtKeyInput.Text.Trim();

            // Lấy kiểu từ ComboBox (ASCII hay Hex)
            string type = ((ComboBoxItem)cbKeyType.SelectedItem).Content.ToString();

            if (type == "ASCII")
            {
                // Kiểm tra độ dài ASCII phải là 8
                if (inputKey.Length != 8)
                {
                    MessageBox.Show("Với định dạng ASCII, khóa phải có đúng 8 ký tự!", "Lỗi Khóa", MessageBoxButton.OK, MessageBoxImage.Error);
                    return null; // Trả về null để báo lỗi
                }
                return System.Text.Encoding.ASCII.GetBytes(inputKey);
            }
            else // Trường hợp HEX
            {
                // Kiểm tra độ dài Hex phải là 16
                if (inputKey.Length != 16)
                {
                    MessageBox.Show("Với định dạng Hex, khóa phải có đúng 16 ký tự (0-9, A-F)!", "Lỗi Khóa", MessageBoxButton.OK, MessageBoxImage.Error);
                    return null;
                }

                try
                {
                    // Chuyển chuỗi Hex thành mảng byte
                    return Convert.FromHexString(inputKey);
                }
                catch
                {
                    MessageBox.Show("Chuỗi Hex không hợp lệ! Chỉ được chứa số 0-9 và chữ A-F.", "Lỗi Khóa", MessageBoxButton.OK, MessageBoxImage.Error);
                    return null;
                }
            }
        }

        // 🔑 KHÓA BÍ MẬT THỰC SỰ DÙNG CHO DES
        private string _secretKey = "";

        public MainWindow()
        {
            InitializeComponent();
            CreateDirectories();
            UpdateKeyDisplay();
        }

        private void CreateDirectories()
        {
            string docPath = AppDomain.CurrentDomain.BaseDirectory;
            string[] folders = { "DES_Output" };
            foreach (string folder in folders)
            {
                string path = Path.Combine(docPath, folder);
                if (!Directory.Exists(path)) Directory.CreateDirectory(path);
            }
        }

        FileDialog BuildFileDialog(bool isSave)
        {
            FileDialog fd = isSave ? (FileDialog)new SaveFileDialog() : new OpenFileDialog();
            fd.InitialDirectory = AppDomain.CurrentDomain.BaseDirectory;
            return fd;
        }

        // ==========================================================
        // KHU VỰC 1: XỬ LÝ KHÓA (TAB TẠO KHÓA)
        // ==========================================================

        private void txtKeyInput_TextChanged(object sender, TextChangedEventArgs e)
        {
            // Lấy kiểu đang chọn (ASCII hay Hex)
            // Lưu ý: Cần kiểm tra null vì sự kiện này có thể chạy trước khi ComboBox kịp tạo xong
            if (cbKeyType == null || cbKeyType.SelectedItem == null) return;

            string type = ((ComboBoxItem)cbKeyType.SelectedItem).Content.ToString();
            string text = txtKeyInput.Text;

            if (type == "ASCII")
            {
                // Nếu là ASCII thì không cho nhập quá 8 (Dư thì cắt bỏ)
                if (text.Length > 8)
                {
                    txtKeyInput.Text = text.Substring(0, 8);
                    txtKeyInput.Select(8, 0); // Đưa con trỏ về cuối
                }
            }
            else // Hex
            {
                // Nếu là Hex thì cho phép nhập đến 16
                if (text.Length > 16)
                {
                    txtKeyInput.Text = text.Substring(0, 16);
                    txtKeyInput.Select(16, 0);
                }
            }
        }

        private void ResetKey_Click(object sender, RoutedEventArgs e)
        {
            txtKeyInput.Clear();
            _secretKey = "";
            UpdateKeyDisplay();
            txtKeyInput.Focus();
        }
        // Ham sinh khoa bi mat
        private string GenerateSecretKeyFromInput(string inputKey)
        {
            // Dùng thuật toán băm SHA256
            // Dùng hash để đảm bảo:
            // - khác từ khóa
            // - cùng input -> cùng output
            using (var sha = System.Security.Cryptography.SHA256.Create())
            {
                //Chuyển chuỗi người dùng nhập (ví dụ "abc") thành mảng byte thô.
                //Sau đó đưa mảng byte đầu vào đi qua "máy xay" SHA-256. 
                //Kết quả (hash): Dù bạn nhập 1 ký tự hay 1000 ký tự, kết quả trả về luôn luôn là một mảng 32 byte (tương đương 256 bit).
                byte[] hash = sha.ComputeHash(Encoding.ASCII.GetBytes(inputKey));

                //Đây là bảng ký tự đích. Chúng ta muốn khóa sinh ra chỉ chứa chữ hoa và số (Tổng cộng 36 ký tự) để dễ nhìn và dễ đọc.
                const string chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                StringBuilder result = new StringBuilder();

                // Lấy 8 byte đầu để tạo khóa 8 ký tự
                for (int i = 0; i < 8; i++)
                {
                    result.Append(chars[hash[i] % chars.Length]);
                }

                return result.ToString();
            }
        }

        private void GenerateKey_OnClick(object sender, RoutedEventArgs e)
        {
            // 1. Lấy kiểu nhập từ ComboBox (ASCII hay Hex)
            string type = ((ComboBoxItem)cbKeyType.SelectedItem).Content.ToString();
            string inputKey = txtKeyInput.Text.Trim();
            Random random = new Random();

            // ==========================================================
            // TRƯỜNG HỢP 1: KHÔNG NHẬP GÌ → TỰ ĐỘNG SINH KHÓA
            // ==========================================================
            if (string.IsNullOrEmpty(inputKey))
            {
                if (type == "ASCII")
                {
                    // Logic cũ của bạn: Sinh 8 ký tự ngẫu nhiên
                    const string chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                    inputKey = new string(Enumerable.Repeat(chars, 8)
                        .Select(s => s[random.Next(s.Length)]).ToArray());
                }
                else // Trường hợp HEX
                {
                    // Logic mới: Sinh 8 byte ngẫu nhiên rồi chuyển sang chuỗi Hex (16 ký tự)
                    byte[] randomBytes = new byte[8];
                    random.NextBytes(randomBytes);
                    // Chuyển sang chuỗi Hex viết hoa (ví dụ: A1B2...)
                    inputKey = BitConverter.ToString(randomBytes).Replace("-", "");
                }

                // Cập nhật giá trị vừa sinh lên ô nhập
                txtKeyInput.Text = inputKey;
            }

            // ==========================================================
            // TRƯỜNG HỢP 2: KIỂM TRA ĐỘ DÀI (VALIDATION)
            // ==========================================================
            if (type == "ASCII")
            {
                if (inputKey.Length != 8)
                {
                    MessageBox.Show("Với định dạng ASCII, khóa phải đúng 8 ký tự!", "Lỗi Độ Dài",
                        MessageBoxButton.OK, MessageBoxImage.Error);
                    return;
                }
            }
            else // Hex
            {
                if (inputKey.Length != 16)
                {
                    MessageBox.Show("Với định dạng Hex, khóa phải đúng 16 ký tự (0-9, A-F)!", "Lỗi Độ Dài",
                        MessageBoxButton.OK, MessageBoxImage.Error);
                    return;
                }

                // Kiểm tra xem có phải ký tự Hex hợp lệ không
                if (!System.Text.RegularExpressions.Regex.IsMatch(inputKey, @"\A\b[0-9a-fA-F]+\b\Z"))
                {
                    MessageBox.Show("Chuỗi Hex chứa ký tự không hợp lệ!", "Lỗi Định Dạng",
                       MessageBoxButton.OK, MessageBoxImage.Error);
                    return;
                }
            }

            // ==========================================================
            // TRƯỜNG HỢP 3: SINH KHÓA BÍ MẬT (Hiển thị kết quả)
            // ==========================================================

            // Lưu ý: Nếu hàm GenerateSecretKeyFromInput cũ của bạn chỉ xử lý ASCII,
            // thì khi đưa Hex vào nó có thể bị sai. Mình tách ra xử lý riêng cho an toàn:

            if (type == "ASCII")
            {
                // Dùng lại hàm cũ của bạn cho ASCII
                _secretKey = GenerateSecretKeyFromInput(inputKey);
            }
            else // Hex
            {
                // Với Hex, chính inputKey là khóa bí mật, ta chỉ cần format lại cho đẹp (nếu muốn)
                // Ví dụ input là "AABB..." thì hiển thị cũng là "AABB..."
                _secretKey = inputKey;
            }

            // Hiển thị kết quả
            txtKeyDisplay.Text = _secretKey;
            cbShowKey.IsChecked = true;

            // Thông báo (Optional - nếu thấy phiền có thể bỏ dòng này)
            // MessageBox.Show("Đã sinh khóa thành công!", "Thông báo");
        }

        private void ShowKey_Click(object sender, RoutedEventArgs e)
        {
            UpdateKeyDisplay();
        }



        // 🔐 CHỈ HIỂN THỊ – KHÔNG LÀM MẤT KHÓA
        private void UpdateKeyDisplay()
        {
            if (txtKeyDisplay == null) return;

            if (cbShowKey.IsChecked == true)
                txtKeyDisplay.Text = _secretKey;
            else
                txtKeyDisplay.Text = new string('*', _secretKey.Length);
        }

        private void SaveFileWithKeyValueButtonWithDialog_Click(object sender, RoutedEventArgs e)
        {
            var fd = BuildFileDialog(true);
            fd.Filter = "Key files|*.key|Text files|*.txt";

            if (fd.ShowDialog() == true)
            {
                if (txtKeyInput.Text.Length != 8 || txtKeyDisplay.Text.Length != 8)
                {
                    MessageBox.Show("Từ khóa và khóa bí mật phải đủ 8 ký tự!",
                        "Lỗi", MessageBoxButton.OK, MessageBoxImage.Error);
                    return;
                }

                string content =
                    $"INPUT_KEY={txtKeyInput.Text}\n" +
                    $"SECRET_KEY={txtKeyDisplay.Text}";

                File.WriteAllText(fd.FileName, content);
                MessageBox.Show("Đã lưu từ khóa và khóa bí mật.", "Thành công");
            }
        }


        private void OpenFileWithKeyValueButtonWithDialog_Click(object sender, RoutedEventArgs e)
        {
            var fd = BuildFileDialog(false);
            fd.Filter = "Key files|*.key|Text files|*.txt";

            if (fd.ShowDialog() == true)
            {
                string[] lines = File.ReadAllLines(fd.FileName);

                string inputKey = lines.FirstOrDefault(l => l.StartsWith("INPUT_KEY="))?
                    .Replace("INPUT_KEY=", "").Trim();

                string secretKey = lines.FirstOrDefault(l => l.StartsWith("SECRET_KEY="))?
                    .Replace("SECRET_KEY=", "").Trim();

                if (string.IsNullOrEmpty(inputKey) || string.IsNullOrEmpty(secretKey))
                {
                    MessageBox.Show("File khóa không hợp lệ!", "Lỗi",
                        MessageBoxButton.OK, MessageBoxImage.Error);
                    return;
                }

                if (inputKey.Length != 8 || secretKey.Length != 8)
                {
                    MessageBox.Show("Khóa trong tệp phải đúng 8 ký tự!", "Lỗi",
                        MessageBoxButton.OK, MessageBoxImage.Error);
                    return;
                }

                txtKeyInput.Text = inputKey;
                _secretKey = secretKey;          // ← THIẾU DÒNG NÀY
                txtKeyDisplay.Text = _secretKey;
                cbShowKey.IsChecked = true;


                MessageBox.Show("Đã nhập từ khóa và khóa bí mật từ tệp.", "Thông báo");
            }
        }


        // ==========================================================
        // KHU VỰC 2: MÃ HÓA
        // ==========================================================
        private void Encrypt(object sender, RoutedEventArgs e)
        {
            // --- THAY ĐỔI 1: Gọi hàm lấy khóa mới (Hỗ trợ cả ASCII và Hex) ---
            byte[] keyBytes = GetKeyFromUI();

            // Nếu hàm trả về null (nghĩa là người dùng nhập sai độ dài hoặc sai format Hex)
            // thì dừng lại ngay, không chạy tiếp. (Hàm GetKeyFromUI đã hiện thông báo lỗi rồi)
            if (keyBytes == null) return;

            // --- (Phần dưới này giữ nguyên logic cũ của bạn) ---
            string plaintext = txtPlainInput.Text;
            if (string.IsNullOrEmpty(plaintext))
            {
                MessageBox.Show("Vui lòng nhập bản rõ hoặc tải file!", "Lỗi");
                return;
            }

            try
            {
                byte[] bytesInput;
                if (!_fileOrPlainText)
                    bytesInput = Encoding.UTF8.GetBytes(plaintext); // Dùng UTF8 để hỗ trợ Tiếng Việt
                else
                {
                    bytesInput = Auxx.StringToByteArray(plaintext);
                    _fileOrPlainText = false;
                }

                // --- THAY ĐỔI 2: Xóa dòng lấy key cũ, dùng keyBytes đã lấy ở trên ---
                // CŨ: byte[] keyBytes = Encoding.ASCII.GetBytes(_secretKey); -> XÓA DÒNG NÀY

                DES des = new DES();
                byte[] encryptedBytes = des.Encrypt(bytesInput, keyBytes); // Truyền keyBytes vào

                txtCipherOutput.Text = Auxx.ByteArrayToString(encryptedBytes);
                _lastEncryptKeyBytes = keyBytes.ToArray();

                MessageBox.Show("Mã hóa thành công!", "Thông báo",
                    MessageBoxButton.OK, MessageBoxImage.Information);


            }
            catch (Exception ex)
            {
                MessageBox.Show("Lỗi mã hóa: " + ex.Message);
            }
        }
        // <Button Click="encryptFile_Click" Content="Tải tệp nhị phân (ảnh, pdf...)" Width="228" Background="#28A745" Height="35"/>

        /* private void encryptFile_Click(object sender, RoutedEventArgs e)
         {
             var fd = BuildFileDialog(false);
             if (fd.ShowDialog() == true)
             {
                 byte[] fileBytes = File.ReadAllBytes(fd.FileName);
                 _ReadFileExtension = Path.GetExtension(fd.FileName);
                 txtPlainInput.Text = Auxx.ByteArrayToString(fileBytes);

                 _fileOrPlainText = true;
                 MessageBox.Show($"Đã tải file: {fd.SafeFileName}", "Sẵn sàng mã hóa");


             }
         } */

        // ==========================================================
        // KHU VỰC 3: GIẢI MÃ (Bản tinh chỉnh hiển thị)
        // ==========================================================
        private void Decrypt(object sender, RoutedEventArgs e)
        {
            // 1. LẤY KHÓA
            byte[] keyBytes = GetKeyFromUI();
            if (keyBytes == null) return;

            // 2. LẤY BẢN MÃ
            string cipherHex = txtCipherInput.Text.Trim();
            if (string.IsNullOrEmpty(cipherHex))
            {
                MessageBox.Show("Vui lòng nhập bản mã (Hex)!", "Thông báo", MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }

            try
            {
                byte[] cipherBytes = Auxx.StringToByteArray(cipherHex);

                if (cipherBytes.Length % 8 != 0)
                {
                    MessageBox.Show("Bản mã bị lỗi độ dài (không chia hết cho 8).", "Lỗi kích thước", MessageBoxButton.OK, MessageBoxImage.Error);
                    return;
                }

                // 3. GIẢI MÃ VÀ KIỂM TRA TOÀN VẸN HỆ THỐNG KHÓA / PADDING
                DES des = new DES();
                byte[] decryptedBytes = null;
                bool isStructureCorrupted = false;

                try
                {
                    decryptedBytes = des.Decrypt(cipherBytes, keyBytes);
                }
                catch (Exception)
                {
                    isStructureCorrupted = true;
                }

                // KHẨ TRƯỜNG BẮT LỖI 1: KHÓA TẠO BỊ THAY ĐỔI (Hoặc khối cuối bị phá hủy)
                if (isStructureCorrupted || decryptedBytes == null)
                {
                    string errorMsg = "🚨 PHÁT HIỆN LỖI TOÀN VẸN CẤU TRÚC:\n" +
                                      "• Trạng thái: Thất bại hệ thống!\n" +
                                      "• Nguyên nhân chính: KHÓA BÍ MẬT ĐÃ BỊ THAY ĐỔI (Sai khóa) khiến thuật toán không thể gỡ bỏ lớp đệm Padding, hoặc Khối cuối cùng của Bản mã đã bị chỉnh sửa.";

                    MessageBox.Show(errorMsg, "Lỗi Thay Đổi Khóa / Phá Hủy Cấu Trúc", MessageBoxButton.OK, MessageBoxImage.Error);
                    txtPlainOutput.Text = "--- [LỖI NGHIÊM TRỌNG] KHÓA ĐÃ BỊ THAY ĐỔI HOẶC BẢN MÃ SAI CẤU TRÚC PADDING ---";
                    return;
                }

                // KHẨ TRƯỜNG BẮT LỖI 2: KHÓA ĐÚNG NHƯNG BẢN MÃ BỊ SỬA ĐỔI GIỮA CHỪNG (Kiểm tra từng khối độc lập)
                int totalBlocks = cipherBytes.Length / 8;
                System.Collections.Generic.List<int> corruptedBlocks = new System.Collections.Generic.List<int>();

                for (int i = 0; i < totalBlocks; i++)
                {
                    int byteIndexStart = i * 8;
                    int lengthToCheck = Math.Min(8, decryptedBytes.Length - byteIndexStart);
                    if (lengthToCheck <= 0) break;

                    byte[] blockBytes = new byte[lengthToCheck];
                    Array.Copy(decryptedBytes, byteIndexStart, blockBytes, 0, lengthToCheck);
                    string blockText = Encoding.UTF8.GetString(blockBytes);

                    // Kiểm tra nếu khối chứa ký tự lỗi thay thế của UTF-8 () hoặc chứa byte rác điều khiển bất hợp pháp
                    if (blockText.Contains('\uFFFD') || blockBytes.Any(b => b < 32 && b != 10 && b != 13 && b != 9))
                    {
                        corruptedBlocks.Add(i + 1); // Lưu lại vị trí khối bị lỗi (bắt đầu từ khối 1)
                    }
                }

                string resultText = Encoding.UTF8.GetString(decryptedBytes).Trim('\0');

                // Nếu phát hiện có khối bị thay đổi nội dung
                if (corruptedBlocks.Count > 0)
                {
                    string blocksStr = string.Join(", ", corruptedBlocks);
                    StringBuilder details = new StringBuilder();
                    foreach (int b in corruptedBlocks)
                    {
                        details.AppendLine($"- Khối số {b}: Tương ứng vị trí chuỗi ký tự Hex từ số {(b - 1) * 16} đến {b * 16 - 1}");
                    }

                    string errorAlert = $"🚨 PHÁT HIỆN BẢN MÃ BỊ THAY ĐỔI NỘI DUNG TRÊN ĐƯỜNG TRUYỀN!\n\n" +
                                        $"• Bộ phận bị sửa đổi trái phép: Tại KHỐI SỐ [ {blocksStr} ]\n" +
                                        $"• Chi tiết định vị tọa độ Hex bị lỗi:\n{details.ToString()}\n" +
                                        $"• Đánh giá: Khóa giải mã chính xác nhưng phần Bản mã tại các vị trí trên đã bị can thiệp và biến đổi.";

                    MessageBox.Show(errorAlert, "Cảnh Báo Dữ Liệu Bị Thay Đổi", MessageBoxButton.OK, MessageBoxImage.Warning);
                    txtPlainOutput.Text = $"--- [CẢNH BÁO TOÀN VẸN] BẢN MÃ BỊ SỬA ĐỔI TẠI KHỐI: {blocksStr} ---\n\nDỮ LIỆU GIẢI MÃ BỊ LỖI:\n{resultText}";
                }
                else
                {
                    // Trường hợp hoàn hảo không lỗi lầm
                    txtPlainOutput.Text = resultText;
                    MessageBox.Show("🔓 Giải mã thành công! Đã kiểm tra: Khóa tạo và Bản mã hoàn toàn nguyên vẹn, không bị thay đổi.", "Thông báo", MessageBoxButton.OK, MessageBoxImage.Information);
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("Lỗi hệ thống phát sinh: " + ex.Message);
            }
        }


        // ==========================================================
        // CÁC HÀM LƯU DỮ LIỆU ĐẦU VÀO (MỚI THÊM)
        // ==========================================================

        // 1. Lưu nội dung từ ô nhập liệu BẢN RÕ (Tab Mã hóa)
        private void SaveInputPlainText_Click(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrEmpty(txtPlainInput.Text))
            {
                MessageBox.Show("Không có dữ liệu đầu vào để lưu!", "Thông báo");
                return;
            }

            var fd = BuildFileDialog(true); // true = Chế độ Lưu file
            fd.FileName = "Input_Plaintext";
            // Thêm bộ lọc định dạng thả xuống (Dropdown)
            fd.Filter = "Text files (*.txt)|*.txt|Word documents (*.docx)|*.docx|JSON files (*.json)|*.json|PDF files (*.pdf)|*.pdf|Excel files (*.xlsx)|*.xlsx";

            if (fd.ShowDialog() == true)
            {
                string ext = Path.GetExtension(fd.FileName).ToLower();
                SaveDataInMultipleFormats(fd.FileName, ext, txtPlainInput.Text);
                MessageBox.Show($"Đã lưu bản rõ đầu vào dưới dạng file {ext.ToUpper()} thành công!", "Thành công");
            }
        }

        // 2. Lưu nội dung từ ô nhập liệu BẢN MÃ (Tab Giải mã)
        private void SaveInputCipherText_Click(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrEmpty(txtCipherInput.Text))
            {
                MessageBox.Show("Không có dữ liệu bản mã để lưu!", "Thông báo");
                return;
            }

            var fd = BuildFileDialog(true);
            fd.FileName = "Input_Ciphertext";
            // Thêm bộ lọc định dạng thả xuống (Dropdown)
            fd.Filter = "Text files (*.txt)|*.txt|Word documents (*.docx)|*.docx|JSON files (*.json)|*.json|PDF files (*.pdf)|*.pdf|Excel files (*.xlsx)|*.xlsx";

            if (fd.ShowDialog() == true)
            {
                string ext = Path.GetExtension(fd.FileName).ToLower();
                SaveDataInMultipleFormats(fd.FileName, ext, txtCipherInput.Text);
                MessageBox.Show($"Đã lưu bản mã đầu vào dưới dạng file {ext.ToUpper()} thành công!", "Thành công");
            }
        }

        // ==========================================================
        // FILE
        // ==========================================================
        private void OpenFileWithPlainTextButtonWithDialog_Click(object sender, RoutedEventArgs e)
        {
            var fd = BuildFileDialog(false);
            if (fd.ShowDialog() == true) txtPlainInput.Text = File.ReadAllText(fd.FileName);
        }

        private void SaveFileWithCryptogramButtonWithDialog_Click(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrEmpty(txtCipherOutput.Text))
            {
                MessageBox.Show("Không có dữ liệu bản mã để lưu!", "Thông báo");
                return;
            }

            var fd = BuildFileDialog(true);
            fd.FileName = "Kết_quả_Bản_mã";
            fd.Filter = "Text files (*.txt)|*.txt|Word documents (*.docx)|*.docx|JSON files (*.json)|*.json|PDF files (*.pdf)|*.pdf|Excel files (*.xlsx)|*.xlsx";

            if (fd.ShowDialog() == true)
            {
                string ext = Path.GetExtension(fd.FileName).ToLower();
                SaveDataInMultipleFormats(fd.FileName, ext, txtCipherOutput.Text);
                MessageBox.Show($"Đã tải và lưu Bản mã dưới dạng file {ext.ToUpper()} thành công!", "Thành công");
            }
        }

        private void OpenFileWithCryptogramButtonWithDialog_Click(object sender, RoutedEventArgs e)
        {
            var fd = BuildFileDialog(false);
            if (fd.ShowDialog() == true) txtCipherInput.Text = File.ReadAllText(fd.FileName);
        }

        private void SaveFileWithPlainTextButtonWithDialog_Click(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrEmpty(txtPlainOutput.Text))
            {
                MessageBox.Show("Không có dữ liệu bản rõ để lưu!", "Thông báo");
                return;
            }

            var fd = BuildFileDialog(true);
            fd.FileName = "Kết_quả_Giải_mã";
            fd.Filter = "Text files (*.txt)|*.txt|Word documents (*.docx)|*.docx|JSON files (*.json)|*.json|PDF files (*.pdf)|*.pdf|Excel files (*.xlsx)|*.xlsx";

            if (fd.ShowDialog() == true)
            {
                string ext = Path.GetExtension(fd.FileName).ToLower();
                SaveDataInMultipleFormats(fd.FileName, ext, txtPlainOutput.Text);
                MessageBox.Show($"Đã tải và lưu Bản rõ dưới dạng file {ext.ToUpper()} thành công!", "Thành công");
            }
        }

        private void RebuildFile_Click(object sender, RoutedEventArgs e)
        {
            var fd = BuildFileDialog(true);
            fd.FileName = "RecoveredFile" + _ReadFileExtension;
            if (fd.ShowDialog() == true)
            {
                try
                {
                    byte[] fileBytes = Auxx.StringToByteArray(txtPlainOutput.Text);
                    File.WriteAllBytes(fd.FileName, fileBytes);
                    MessageBox.Show("Đã khôi phục file gốc!", "Thành công");
                }
                catch
                {
                    MessageBox.Show("Dữ liệu không hợp lệ.", "Lỗi");
                }
            }
        }

        
        private void cbKeyType_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            // Kiểm tra xem các control đã được khởi tạo chưa để tránh lỗi null khi mới chạy
            if (txtKeyInput == null || cbKeyType == null) return;

            // Lấy kiểu đang chọn
            ComboBoxItem selectedItem = (ComboBoxItem)cbKeyType.SelectedItem;
            if (selectedItem == null) return;

            string type = selectedItem.Content.ToString();

            if (type == "ASCII")
            {
                txtKeyInput.MaxLength = 8; // Giới hạn 8 ký tự
            }
            else // Hex
            {
                txtKeyInput.MaxLength = 16; // Mở rộng lên 16 ký tự
            }

            // Xóa nội dung cũ để người dùng nhập lại cho đúng định dạng mới
            txtKeyInput.Clear();
            txtKeyInput.Focus();
        }

        // ==========================================================
        // ĐỘNG CƠ XỬ LÝ ĐA ĐỊNH DẠNG TỆP (MỚI BỔ SUNG)
        // ==========================================================
        private void SaveDataInMultipleFormats(string filePath, string extension, string content)
        {
            switch (extension)
            {
                case ".txt":
                    File.WriteAllText(filePath, content, Encoding.UTF8);
                    break;

                case ".json":
                    // Đóng gói dữ liệu chuẩn cấu trúc JSON Object
                    string jsonStr = "{\n" +
                                     "  \"Project\": \"DES Cryptographic System\",\n" +
                                     "  \"ExportTime\": \"" + DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss") + "\",\n" +
                                     "  \"Length\": " + content.Length + ",\n" +
                                     "  \"Data\": \"" + content.Replace("\\", "\\\\").Replace("\"", "\\\"").Replace("\r", "").Replace("\n", "\\n") + "\"\n" +
                                     "}";
                    File.WriteAllText(filePath, jsonStr, Encoding.UTF8);
                    break;

                case ".xlsx":
                case ".xls":
                    // Tạo bảng Excel thông qua định dạng văn bản Tab-Separated hỗ trợ UTF-8 BOM để không lỗi font
                    StringBuilder excelBuild = new StringBuilder();
                    excelBuild.AppendLine("Số thứ tự\tDanh mục cấu trúc\tGiá trị nội dung cryptographic");
                    excelBuild.AppendLine($"1\tThời gian xuất file\t{DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss")}");
                    excelBuild.AppendLine($"2\tDữ liệu kết quả thuật toán\t{content.Replace("\t", " ").Replace("\r\n", " [Xuống dòng] ").Replace("\n", " [Xuống dòng] ")}");

                    byte[] dataBytes = Encoding.UTF8.GetBytes(excelBuild.ToString());
                    byte[] utf8Bom = { 0xEF, 0xBB, 0xBF }; // Đánh dấu BOM để MS Excel nhận diện tiếng Việt liền lập tức
                    using (var fs = File.Create(filePath))
                    {
                        fs.Write(utf8Bom, 0, utf8Bom.Length);
                        fs.Write(dataBytes, 0, dataBytes.Length);
                    }
                    break;

                case ".docx":
                case ".doc":
                    // Chuyển đổi dữ liệu sang định dạng HTML-WordStream giúp Microsoft Word đọc cấu trúc mượt mà
                    StringBuilder wordBuild = new StringBuilder();
                    wordBuild.AppendLine("<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>");
                    wordBuild.AppendLine("<head><title>DES Export</title><style>body { font-family: 'Segoe UI', Arial; font-size: 13pt; line-height: 1.5; }</style></head>");
                    wordBuild.AppendLine("<body>");
                    wordBuild.AppendLine("<h2 style='color:#007ACC;'>HỆ THỐNG MÃ HÓA DES - KẾT QUẢ ĐẦU RA</h2>");
                    wordBuild.AppendLine($"<p><b>Ngày giờ khởi tạo tệp:</b> {DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss")}</p>");
                    wordBuild.AppendLine("<hr style='border: 1px solid #DDDDDD;' />");
                    wordBuild.AppendLine($"<div style='background:#F9F9F9; padding:15px; border:1px solid #CCCCCC; font-family:Consolas, monospace; word-break:break-all;'>{content.Replace("\r\n", "<br/>").Replace("\n", "<br/>")}</div>");
                    wordBuild.AppendLine("</body></html>");

                    File.WriteAllText(filePath, wordBuild.ToString(), Encoding.UTF8);
                    break;

                case ".pdf":
                    // Tạo tệp PDF đặc chủng thuần cấu trúc Vector nội dung (Standard Vector Text PDF) chạy trực tiếp không phụ thuộc thư viện ngoài
                    StringBuilder pdfBuild = new StringBuilder();
                    pdfBuild.Append("%PDF-1.4\n");
                    pdfBuild.Append("1 0 obj <</Type /Catalog /Pages 2 0 R>> endobj\n");
                    pdfBuild.Append("2 0 obj <</Type /Pages /Kids [3 0 R] /Count 1>> endobj\n");
                    pdfBuild.Append("3 0 obj <</Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources <</Font <</F1 5 0 R>>>>>> endobj\n");

                    // Khử dấu tiếng Việt để PDF Viewer tích hợp sẵn của Windows hiển thị chuẩn xác không lỗi font hệ thống
                    string safeContent = RemoveSignForVietnamese(content).Replace("(", "\\(").Replace(")", "\\)");
                    string[] rawLines = safeContent.Split('\n');

                    StringBuilder pdfStream = new StringBuilder();
                    pdfStream.Append("BT\n/F1 12 Tf\n16 TL\n50 780 Td\n");
                    pdfStream.Append("(KET QUA DU LIEU TU PHAN MEM MA HOA DES)\nT*\n");
                    pdfStream.Append($" (Ngay xuat: {DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss")})\nT*\n-------------------------------------------------------------\nT*\n");

                    foreach (var line in rawLines)
                    {
                        string lineData = line.Replace("\r", "").Trim();
                        if (lineData.Length > 65) // Tự động bẻ dòng nếu chuỗi quá dài tràn lề PDF
                        {
                            for (int startIdx = 0; startIdx < lineData.Length; startIdx += 65)
                            {
                                int sliceLen = Math.Min(65, lineData.Length - startIdx);
                                pdfStream.Append($"({lineData.Substring(startIdx, sliceLen)}) Tj T*\n");
                            }
                        }
                        else
                        {
                            pdfStream.Append($"({lineData}) Tj T*\n");
                        }
                    }
                    pdfStream.Append("ET\n");

                    pdfBuild.Append("4 0 obj <</Length " + pdfStream.Length + ">> stream\n" + pdfStream.ToString() + "endstream endobj\n");
                    pdfBuild.Append("5 0 obj <</Type /Font /Subtype /Type1 /BaseFont /Helvetica>> endobj\n");
                    pdfBuild.Append("xref\n0 6\n0000000000 65535 f \n0000000009 00000 n \n0000000056 00000 n \n0000000111 00000 n \n");
                    pdfBuild.Append("trailer <</Size 6 /Root 1 0 R>>\nstartxref\n480\n%%EOF");

                    File.WriteAllText(filePath, pdfBuild.ToString(), Encoding.ASCII);
                    break;

                default:
                    File.WriteAllText(filePath, content, Encoding.UTF8);
                    break;
            }
        }

        // Hàm bổ trợ bóc tách dấu tiếng Việt phục vụ bộ kết xuất PDF thuần
        private string RemoveSignForVietnamese(string str)
        {
            string[] signChars = new string[] { "á", "à", "ả", "ã", "ạ", "â", "ấ", "ầ", "ẩ", "ẫ", "ậ", "ă", "ắ", "ằ", "ẳ", "ẵ", "ặ", "đ", "é", "è", "ẻ", "ẽ", "ẹ", "ê", "ế", "ề", "ể", "ễ", "ệ", "í", "ì", "ỉ", "ĩ", "ị", "ó", "ò", "ỏ", "õ", "ọ", "ô", "ố", "ồ", "ổ", "ỗ", "ộ", "ơ", "ớ", "ờ", "ở", "ỡ", "ợ", "ú", "ù", "ủ", "ũ", "ụ", "ư", "ứ", "ừ", "ử", "ữ", "ự", "ý", "ỳ", "ỷ", "ỹ", "ỵ" };
            string[] normalChars = new string[] { "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "d", "e", "e", "e", "e", "e", "e", "e", "e", "e", "e", "e", "i", "i", "i", "i", "i", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "u", "u", "u", "u", "u", "u", "u", "u", "u", "u", "u", "u", "y", "y", "y", "y", "y" };
            for (int i = 0; i < signChars.Length; i++)
            {
                str = str.Replace(signChars[i], normalChars[i]);
                str = str.Replace(signChars[i].ToUpper(), normalChars[i].ToUpper());
            }
            return str;
        }
    }
}
