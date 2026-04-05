package com.example.mamacook;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SampleData {

    public static void uploadAll(FirebaseFirestore db) {
        List<MonAn> samples = new ArrayList<>();

        // --- MÓN MẶN (mon_man) ---
        samples.add(createSample("Thịt Kho Tàu", "https://i.ytimg.com/vi/H6ZeR1poY-Q/maxresdefault.jpg", 60, "Trung bình", "mon_man", 3000, 4.9, 200,
                new Object[][]{{"Thịt ba chỉ", 500, "g"}, {"Trứng vịt", 5, "quả"}, {"Nước dừa", 500, "ml"}, {"Hành tím", 3, "củ"}, {"Tỏi", 1, "củ"}},
                new String[]{"Thịt ba chỉ thái vuông, ướp với mắm, đường, hành tỏi băm trong 30 phút.", 
                            "Luộc trứng vịt chín, bóc vỏ.", 
                            "Cho thịt vào nồi xào săn lại, sau đó cho nước dừa vào ngập mặt thịt.", 
                            "Kho lửa nhỏ cho đến khi thịt mềm, cho trứng vào kho chung đến khi nước sền sệt."}));

        samples.add(createSample("Sườn Xào Chua Ngọt", "https://cdn.tgdd.vn/Files/2017/04/10/970784/suon-xao-chua-ngot-kieu-mien-bac-va-mien-nam-cach-nao-ngon-hon-202206031021464303.jpg", 40, "Dễ", "mon_man", 2500, 4.8, 150,
                new Object[][]{{"Sườn non", 500, "g"}, {"Hành tây", 1, "củ"}, {"Ớt chuông", 1, "quả"}, {"Dứa", 1, "miếng"}, {"Cà chua", 2, "quả"}},
                new String[]{"Sườn rửa sạch, chần nước sôi, để ráo rồi rán vàng đều.", 
                            "Pha nước sốt: nước mắm, dấm, đường, tương cà trộn đều.", 
                            "Phi thơm tỏi, cho sườn và nước sốt vào đảo đều.", 
                            "Cho hành tây, ớt chuông, dứa vào xào cùng đến khi nước sốt sệt lại bám đều vào sườn."}));

        samples.add(createSample("Gà Kho Gừng", "https://cdn.tgdd.vn/Files/2017/03/24/964631/cach-lam-ga-kho-gung-ngon-dam-da-dua-com-202206031025281143.jpg", 35, "Dễ", "mon_man", 1500, 4.7, 80,
                new Object[][]{{"Thịt gà", 600, "g"}, {"Gừng tươi", 1, "nhánh"}, {"Tỏi", 3, "tép"}, {"Hành tím", 2, "củ"}},
                new String[]{"Gà chặt miếng vừa ăn, ướp với gia vị và một nửa phần gừng thái sợi.", 
                            "Phi thơm hành tỏi và phần gừng còn lại.", 
                            "Cho gà vào xào săn, thêm chút nước màu và nước lọc.", 
                            "Kho lửa nhỏ đến khi gà chín mềm và nước cạn sệt lại."}));

        samples.add(createSample("Cá Lóc Kho Tộ", "https://cdn.tgdd.vn/Files/2019/10/25/1212351/cach-lam-ca-kho-to-thom-ngon-dam-da-chuan-vi-mien-tay-202112311452285496.jpg", 50, "Trung bình", "mon_man", 1800, 4.6, 90,
                new Object[][]{{"Cá lóc", 1, "con"}, {"Thịt ba chỉ", 100, "g"}, {"Hành lá", 3, "nhánh"}, {"Ớt hiểm", 2, "quả"}},
                new String[]{"Cá làm sạch, cắt khúc, ướp nước màu, nước mắm, đường, tiêu và ớt.", 
                            "Thịt ba chỉ thái mỏng, rán sơ cho ra bớt mỡ rồi lót dưới đáy tộ.", 
                            "Xếp cá lên trên thịt, rưới nước ướp vào.", 
                            "Kho lửa nhỏ trong tộ đất cho đến khi nước cá sánh lại, rắc thêm hành lá và tiêu."}));

        samples.add(createSample("Bò Lúc Lắc", "https://cdn.tgdd.vn/Files/2018/12/11/1137331/cach-lam-bo-luc-lac-mem-ngon-dung-dieu-nha-hang-202201041014111513.jpg", 25, "Dễ", "mon_man", 4000, 4.9, 300,
                new Object[][]{{"Thịt bò", 400, "g"}, {"Khoai tây", 2, "củ"}, {"Hành tây", 1, "củ"}, {"Ớt chuông", 1, "quả"}, {"Bơ", 1, "muỗng"}},
                new String[]{"Thịt bò thái quân cờ, ướp dầu hào, tỏi và gia vị.", 
                            "Khoai tây thái sợi chiên giòn.", 
                            "Đun nóng chảo, cho bơ và tỏi vào phi thơm, trút bò vào áp chảo lửa lớn cho cháy cạnh.", 
                            "Cho hành tây và ớt chuông vào đảo nhanh tay rồi tắt bếp, dùng kèm khoai tây chiên."}));

        // --- MÓN CANH (mon_canh) ---
        samples.add(createSample("Canh Cua Mồng Tơi", "https://cdn.tgdd.vn/Files/2017/03/31/967141/cach-nau-canh-cua-rau-mong-toi-ngon-ngot-mat-cho-ngay-he-202206031022214303.jpg", 40, "Khó", "mon_canh", 1300, 4.7, 55,
                new Object[][]{{"Cua đồng", 300, "g"}, {"Rau mồng tơi", 1, "bó"}, {"Rau dền", 1, "bó"}, {"Mướp hương", 1, "quả"}},
                new String[]{"Cua đồng làm sạch, giã nhỏ, lọc lấy nước cốt.", 
                            "Đun nước lọc cua với chút muối, khuấy đều tay đến khi thịt cua bắt đầu đóng màng thì ngừng khuấy.", 
                            "Khi gạch cua đóng thành mảng, gạt nhẹ sang một bên rồi cho rau mồng tơi, rau dền và mướp vào.", 
                            "Nấu chín rau, nêm gia vị vừa ăn là hoàn thành."}));

        samples.add(createSample("Canh Chua Cá Lóc", "https://img-global.cpcdn.com/recipes/572c830b064f2601/680x482cq70/canh-chua-ca-loc-recipe-main-photo.jpg", 45, "Trung bình", "mon_canh", 2200, 4.8, 120,
                new Object[][]{{"Cá lóc", 1, "khúc"}, {"Dứa", 1, "miếng"}, {"Cà chua", 2, "quả"}, {"Me chua", 1, "vắt"}, {"Đậu bắp", 5, "quả"}},
                new String[]{"Làm sạch cá lóc. Đun sôi nước với me chua rồi lọc lấy nước cốt.", 
                            "Cho cá vào nấu chín rồi vớt ra để riêng tránh bị nát.", 
                            "Cho dứa, cà chua, đậu bắp vào nồi nấu sôi.", 
                            "Cuối cùng cho cá vào lại nồi, nêm rau ngổ, ngò gai rồi tắt bếp."}));

        samples.add(createSample("Canh Khổ Qua Nhồi Thịt", "https://cdn.tgdd.vn/Files/2019/04/10/1160166/cach-lam-canh-kho-qua-nhoi-thit-khong-dang-giu-duoc-mau-xanh-202112311450201140.jpg", 50, "Trung bình", "mon_canh", 1100, 4.5, 40,
                new Object[][]{{"Khổ qua", 3, "quả"}, {"Thịt heo băm", 200, "g"}, {"Mộc nhĩ", 2, "tai"}, {"Hành lá", 2, "nhánh"}},
                new String[]{"Khổ qua rửa sạch, bỏ ruột.", 
                            "Trộn thịt băm với mộc nhĩ thái nhỏ và gia vị, sau đó nhồi vào khổ qua.", 
                            "Đun sôi nước, thả khổ qua vào hầm.", 
                            "Hầm lửa nhỏ cho đến khi khổ qua chín mềm, rắc hành lá."}));

        samples.add(createSample("Canh Bí Đỏ Thịt Băm", "https://cdn.tgdd.vn/Files/2017/03/24/964644/cach-nau-canh-bi-do-thit-bam-ngon-ngot-bo-duong-cho-ca-nha-202206031025281143.jpg", 30, "Dễ", "mon_canh", 900, 4.6, 35,
                new Object[][]{{"Bí đỏ", 300, "g"}, {"Thịt heo băm", 100, "g"}, {"Hành tím", 2, "củ"}, {"Ngò ôm", 1, "ít"}},
                new String[]{"Bí đỏ gọt vỏ, thái miếng. Thịt băm ướp gia vị.", 
                            "Phi thơm hành tím, cho thịt băm vào xào săn.", 
                            "Cho nước vào đun sôi.", 
                            "Cho bí đỏ vào nấu đến khi chín mềm, nêm gia vị và ngò ôm."}));

        samples.add(createSample("Canh Rau Ngót Thịt Bằm", "https://cdn.tgdd.vn/Files/2017/03/31/967131/cach-nau-canh-rau-ngot-thit-bam-thom-mat-bo-duong-202206031022214303.jpg", 20, "Dễ", "mon_canh", 800, 4.5, 20,
                new Object[][]{{"Rau ngót", 1, "bó"}, {"Thịt heo băm", 100, "g"}, {"Hành tím", 1, "củ"}},
                new String[]{"Rau ngót tuốt lá, rửa sạch và vò hơi nát.", 
                            "Phi thơm hành tím, xào thịt băm chín.", 
                            "Thêm nước vào nồi đun sôi.", 
                            "Cho rau ngót vào nấu sôi lại khoảng 2 phút, nêm gia vị."}));

        // --- MÓN CHAY (mon_chay) ---
        samples.add(createSample("Đậu Hũ Sốt Cà Chua", "https://cdn.tgdd.vn/Files/2017/03/21/963231/cach-lam-dau-hu-sot-ca-chua-ngon-mieng-de-lam-202206031024501228.jpg", 20, "Dễ", "mon_chay", 3200, 4.9, 210,
                new Object[][]{{"Đậu hũ trắng", 4, "miếng"}, {"Cà chua", 3, "quả"}, {"Hành lá", 2, "nhánh"}},
                new String[]{"Đậu hũ thái miếng, chiên vàng.", 
                            "Cà chua băm nhỏ, xào mềm thành sốt.", 
                            "Cho đậu hũ vào sốt cà chua, thêm gia vị.", 
                            "Rim nhỏ lửa trong 5 phút cho thấm, rắc hành lá."}));

        samples.add(createSample("Nấm Rơm Kho Quẹt", "https://cookbeo.com/media/2021/04/nam-rom-kho-quet/nam-rom-kho-quet.jpg", 30, "Dễ", "mon_chay", 1500, 4.8, 100,
                new Object[][]{{"Nấm rơm", 300, "g"}, {"Tiêu xanh", 2, "nhánh"}, {"Ớt hiểm", 2, "quả"}, {"Nước mắm chay", 3, "muỗng"}},
                new String[]{"Nấm rơm làm sạch, để ráo.", 
                            "Pha hỗn hợp nước mắm chay, đường, tiêu.", 
                            "Cho nấm vào nồi đất kho với lửa nhỏ.", 
                            "Kho đến khi nước cạn sánh, thêm tiêu xanh."}));

        samples.add(createSample("Rau Củ Luộc Kho Quẹt", "https://cdn.tgdd.vn/Files/2019/01/04/1142584/cach-lam-kho-quet-chay-cham-rau-cu-luộc-ngon-ngat-ngay-202201041014411513.jpg", 25, "Dễ", "mon_chay", 1200, 4.7, 60,
                new Object[][]{{"Bông cải xanh", 1, "cây"}, {"Cà rốt", 1, "củ"}, {"Đậu hũ trắng", 2, "miếng"}, {"Nước mắm chay", 4, "muỗng"}},
                new String[]{"Rau củ cắt miếng, luộc chín.", 
                            "Đậu hũ cắt hạt lựu, chiên giòn làm tóp mỡ chay.", 
                            "Làm nước sốt kho quẹt từ mắm chay và đường.", 
                            "Trộn tóp mỡ đậu hũ vào nước sốt, dùng để chấm rau củ."}));

        samples.add(createSample("Canh Nấm Hạt Sen", "https://cdn.tgdd.vn/Files/2018/12/11/1137324/cach-nau-canh-nam-hat-sen-chay-thanh-dam-bo-duong-202201041014111513.jpg", 40, "Trung bình", "mon_chay", 950, 4.6, 45,
                new Object[][]{{"Hạt sen tươi", 100, "g"}, {"Nấm hương", 50, "g"}, {"Cà rốt", 1, "củ"}, {"Đậu hũ", 1, "miếng"}},
                new String[]{"Hạt sen rửa sạch. Nấm hương ngâm nở.", 
                            "Hầm hạt sen đến khi mềm.", 
                            "Cho cà rốt, nấm và đậu hũ vào nấu chín.", 
                            "Nêm gia vị chay thanh đạm."}));

        samples.add(createSample("Chả Giò Chay", "https://cdn.tgdd.vn/Files/2017/03/24/964624/cach-lam-cha-gio-chay-gion-rum-thom-ngon-kho-cuong-202206031025281143.jpg", 45, "Khó", "mon_chay", 2000, 4.8, 130,
                new Object[][]{{"Bánh đa nem", 1, "xấp"}, {"Khoai môn", 1, "củ"}, {"Cà rốt", 1, "củ"}, {"Mộc nhĩ", 3, "tai"}},
                new String[]{"Khoai môn, cà rốt bào sợi. Mộc nhĩ thái nhỏ.", 
                            "Trộn các nguyên liệu làm nhân.", 
                            "Cuốn nhân vào bánh đa nem.", 
                            "Chiên chả giò vàng giòn."}));

        // --- ĂN VẶT (an_vat) ---
        samples.add(createSample("Bánh Tráng Trộn", "https://cdn.tgdd.vn/Files/2019/12/26/1228574/cach-lam-banh-trang-tron-de-ban-ngon-chuan-vi-sai-gon-202112311029177243.jpg", 15, "Dễ", "an_vat", 5000, 4.9, 450,
                new Object[][]{{"Bánh tráng sợi", 200, "g"}, {"Trứng cút", 5, "quả"}, {"Xoài xanh", 1, "quả"}, {"Bò khô", 50, "g"}},
                new String[]{"Xoài bào sợi, trứng cút luộc.", 
                            "Cho bánh tráng, xoài, bò khô vào tô.", 
                            "Thêm muối tôm, tắc, sa tế trộn đều.", 
                            "Thêm lạc rang và rau răm."}));

        samples.add(createSample("Chân Gà Sả Tắc", "https://cdn.tgdd.vn/Files/2020/05/20/1257125/cach-lam-chan-ga-ngam-sa-tac-ngon-gion-tham-vi-khong-bi-dang-202112311036069094.jpg", 40, "Trung bình", "an_vat", 4500, 4.9, 320,
                new Object[][]{{"Chân gà", 500, "g"}, {"Sả", 5, "cây"}, {"Tắc", 10, "quả"}, {"Ớt", 5, "quả"}},
                new String[]{"Luộc chín chân gà với sả gừng, ngâm đá.", 
                            "Pha nước mắm đường tắc.", 
                            "Trộn chân gà với sả, tắc, ớt và nước mắm.", 
                            "Để thấm trong tủ lạnh 4 tiếng."}));

        samples.add(createSample("Gỏi Khô Bò", "https://cdn.tgdd.vn/Files/2017/03/24/964614/cach-lam-goi-kho-bo-ngon-gion-chuan-vi-sai-gon-202206031025281143.jpg", 20, "Dễ", "an_vat", 1800, 4.7, 110,
                new Object[][]{{"Đu đủ xanh", 1, "quả"}, {"Khô bò", 100, "g"}, {"Lạc rang", 50, "g"}, {"Nước mắm", 2, "muỗng"}},
                new String[]{"Đu đủ bào sợi giòn.", 
                            "Pha nước mắm chua ngọt.", 
                            "Xếp đu đủ và khô bò ra đĩa.", 
                            "Rưới nước mắm và thêm lạc rang."}));

        samples.add(createSample("Bánh Tráng Nướng", "https://cdn.tgdd.vn/Files/2017/03/31/967124/cach-lam-banh-trang-nuong-da-lat-thom-ngon-gion-tan-202206031022214303.jpg", 10, "Dễ", "an_vat", 2800, 4.8, 140,
                new Object[][]{{"Bánh tráng", 5, "cái"}, {"Trứng gà", 2, "quả"}, {"Hành lá", 2, "nhánh"}, {"Xúc xích", 2, "cây"}},
                new String[]{"Đặt bánh tráng lên bếp.", 
                            "Phết trứng, hành lá và xúc xích lên mặt.", 
                            "Nướng đến khi vàng giòn.", 
                            "Gập đôi và thưởng thức."}));

        samples.add(createSample("Xoài Lắc", "https://cdn.tgdd.vn/Files/2017/03/31/967114/cach-lam-xoai-lac-ngon-ba-chay-an-hoai-khong-ngan-202206031022214303.jpg", 15, "Dễ", "an_vat", 3500, 4.9, 250,
                new Object[][]{{"Xoài xanh", 2, "quả"}, {"Muối tôm", 2, "muỗng"}, {"Đường", 1, "muỗng"}, {"Ớt bột", 1, "ít"}},
                new String[]{"Xoài cắt miếng vừa ăn.", 
                            "Cho xoài vào hũ.", 
                            "Thêm muối, đường, ớt bột.", 
                            "Lắc đều tay trong 1 phút."}));

        // --- MÓN LẨU (mon_lau) ---
        samples.add(createSample("Lẩu Gà Lá Giang", "https://cdn.tgdd.vn/Files/2018/04/18/1083049/cach-nau-lau-ga-la-giang-ngon-dung-dieu-nhu-ngoai-hang-202203021437142825.jpg", 60, "Khó", "mon_lau", 1200, 4.6, 80,
                new Object[][]{{"Gà ta", 1, "con"}, {"Lá giang", 1, "bó"}, {"Sả", 5, "cây"}, {"Ớt", 3, "quả"}, {"Măng chua", 200, "g"}},
                new String[]{"Gà chặt miếng, xào săn với sả.", 
                            "Cho nước vào đun sôi cho gà chín.", 
                            "Lá giang vò nát cho vào nồi.", 
                            "Nêm gia vị và dùng kèm bún."}));

        samples.add(createSample("Lẩu Thái Hải Sản", "https://cdn.tgdd.vn/Files/2017/03/23/964344/cach-nau-lau-thai-hai-san-chua-cay-ngon-dung-dieu-tai-nha-202201041113061614.jpg", 60, "Khó", "mon_lau", 3500, 4.9, 220,
                new Object[][]{{"Tôm", 300, "g"}, {"Mực", 300, "g"}, {"Nghêu", 500, "g"}, {"Sả", 4, "cây"}, {"Gói gia vị lẩu", 1, "gói"}},
                new String[]{"Hầm xương lấy nước dùng.", 
                            "Phi thơm sả, cho nước dùng vào.", 
                            "Nêm gia vị lẩu Thái.", 
                            "Khi ăn nhúng hải sản và rau."}));

        samples.add(createSample("Lẩu Riêu Cua Bắp Bò", "https://cdn.tgdd.vn/Files/2018/12/11/1137311/cach-nau-lau-rieu-cua-bap-bo-thom-ngon-dung-dieu-cho-ca-nha-202201041014111513.jpg", 70, "Khó", "mon_lau", 2500, 4.8, 160,
                new Object[][]{{"Cua đồng", 500, "g"}, {"Bắp bò", 400, "g"}, {"Đậu hũ", 3, "miếng"}, {"Cà chua", 3, "quả"}},
                new String[]{"Nấu riêu cua nổi lên.", 
                            "Xào cà chua tạo màu.", 
                            "Làm nước lẩu chua thanh với dấm bỗng.", 
                            "Nhúng bò và đậu hũ khi ăn."}));

        samples.add(createSample("Lẩu Nấm Chay", "https://cdn.tgdd.vn/Files/2018/12/11/1137304/cach-nau-lau-nam-chay-thanh-dam-bo-duong-cho-gia-dinh-202201041014111513.jpg", 45, "Trung bình", "mon_lau", 900, 4.7, 50,
                new Object[][]{{"Nấm kim châm", 200, "g"}, {"Nấm đùi gà", 200, "g"}, {"Cà rốt", 1, "củ"}, {"Củ cải trắng", 1, "củ"}},
                new String[]{"Ninh củ quả lấy nước ngọt.", 
                            "Sơ chế các loại nấm.", 
                            "Nêm nước lẩu thanh đạm.", 
                            "Nhúng nấm vào và thưởng thức."}));

        samples.add(createSample("Lẩu Cá Đuối", "https://cdn.tgdd.vn/Files/2019/10/25/1212344/cach-lam-lau-ca-duoi-ngon-chuan-vi-vung-tau-202112311452285496.jpg", 55, "Khó", "mon_lau", 1100, 4.5, 40,
                new Object[][]{{"Cá đuối", 1, "kg"}, {"Măng chua", 300, "g"}, {"Me", 1, "vắt"}, {"Sả", 3, "cây"}},
                new String[]{"Cá đuối làm sạch, thái miếng.", 
                            "Nấu nước dùng với măng chua và me.", 
                            "Cho cá vào nấu chín, nêm đậm đà.", 
                            "Ăn kèm với bún và rau mầm."}));

        // --- ĐẨY LÊN FIRESTORE ---
        for (MonAn mon : samples) {
            db.collection("mon_an").add(mon);
        }
    }

    private static MonAn createSample(String ten, String anh, int tg, String dokho, String danhmuc, int xem, double rate, int danhgia, Object[][] nls, String[] buocs) {
        MonAn mon = new MonAn();
        mon.setTen_mon(ten);
        mon.setHinh_anh(anh);
        mon.setThoi_gian_nau(tg);
        mon.setDo_kho(dokho);
        mon.setId_danh_muc(danhmuc);
        mon.setLuot_xem(xem);
        mon.setRating(rate);
        mon.setTong_luot_danh_gia(danhgia);
        mon.setTrang_thai("hiển thị");
        mon.setNgay_tao(Timestamp.now());
        
        List<MonAn.ChiTietNguyenLieu> nguyenLieuList = new ArrayList<>();
        List<String> keywords = new ArrayList<>();
        
        // TỐI ƯU TÌM KIẾM: Lưu cả có dấu và KHÔNG DẤU
        String cleanTen = ten.toLowerCase(Locale.getDefault());
        String noToneTen = VNCharacterUtils.removeAccents(cleanTen);
        
        for(String w : cleanTen.split("\\s+")) if(!keywords.contains(w)) keywords.add(w);
        for(String w : noToneTen.split("\\s+")) if(!keywords.contains(w)) keywords.add(w);

        for (Object[] nlData : nls) {
            MonAn.ChiTietNguyenLieu nl = new MonAn.ChiTietNguyenLieu();
            nl.ten_nguyen_lieu = (String) nlData[0];
            nl.so_luong = ((Number) nlData[1]).intValue();
            nl.don_vi = (String) nlData[2];
            nguyenLieuList.add(nl);
            
            String cleanNL = nl.ten_nguyen_lieu.toLowerCase(Locale.getDefault());
            String noToneNL = VNCharacterUtils.removeAccents(cleanNL);
            for(String w : cleanNL.split("\\s+")) if(!keywords.contains(w)) keywords.add(w);
            for(String w : noToneNL.split("\\s+")) if(!keywords.contains(w)) keywords.add(w);
        }
        mon.setDanh_sach_nguyen_lieu(nguyenLieuList);

        List<MonAn.BuocNau> buocNauList = new ArrayList<>();
        for (int i = 0; i < buocs.length; i++) {
            MonAn.BuocNau bn = new MonAn.BuocNau();
            bn.so_thu_tu = i + 1;
            bn.noi_dung_buoc = buocs[i];
            buocNauList.add(bn);
        }
        mon.setDanh_sach_buoc_nau(buocNauList);
        
        mon.setTu_khoa_tim_kiem(keywords);
        return mon;
    }
}
