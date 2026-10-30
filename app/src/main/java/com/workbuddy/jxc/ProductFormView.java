package com.workbuddy.jxc;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ProductFormView {
    public static final int REQUEST_CAMERA = 5201;
    public static final int REQUEST_GALLERY = 5202;
    private static final String[] SEASON_LABELS = {"四季款", "春", "夏", "秋", "冬"};
    private static final String[] SEASON_CODES = {"ALL", "SPRING", "SUMMER", "AUTUMN", "WINTER"};
    private static final String[] CATEGORIES = {"连衣裙", "上衣", "裤装", "半身裙", "外套", "针织",
            "衬衫", "T恤", "卫衣", "风衣", "西装", "套装", "牛仔", "其他"};
    private static final String[] STYLES = {"通勤", "休闲", "甜美", "韩系", "法式", "简约", "复古",
            "辣妹", "森系", "淑女", "气质", "清新", "优雅", "小香风", "学院风", "新中式"};

    private final Activity activity;
    private final LinearLayout view;
    private final EditText nameInput;
    private final EditText purchaseInput;
    private final EditText saleInput;
    private final EditText stockInput;
    private final Spinner seasonInput;
    private final AutoCompleteTextView styleInput;
    private final AutoCompleteTextView categoryInput;
    private final ImageView imagePreview;
    private final LinearLayout thumbnails;
    private final TextView photoCount;
    private final List<String> photoPaths = new ArrayList<>();
    private Uri cameraUri;

    public ProductFormView(Activity activity, Product product, ProductDraft draft, boolean showStock) {
        this.activity = activity;
        view = Ui.vertical(activity);
        nameInput = field("商品名称", Ui.input(activity, "例如：法式连衣裙", false));
        purchaseInput = field("进货价（元）", Ui.decimalInput(activity, "例如：60"));
        saleInput = field("销售价（元）", Ui.decimalInput(activity, "例如：70"));
        stockInput = showStock ? field("初始库存（件）", Ui.integerInput(activity, "例如：20")) : null;
        seasonInput = spinner("季节", SEASON_LABELS);
        styleInput = autoCompleteField("风格（可直接输入）", STYLES, "例如：通勤 / 法式 / 新中式");
        categoryInput = autoCompleteField("商品分类（可直接输入）", CATEGORIES, "例如：连衣裙 / 衬衫 / 自定义分类");

        TextView photoLabel = Ui.text(activity, "商品图片（可选，最多9张）", 14, Ui.MUTED, true);
        Ui.add(view, photoLabel, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);
        imagePreview = new ImageView(activity);
        imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagePreview.setBackground(Ui.roundRect(Ui.BACKGROUND, 12, Ui.BORDER, activity));
        Ui.add(view, imagePreview, Ui.dp(activity, 140), Ui.dp(activity, 150), 6);
        thumbnails = Ui.horizontal(activity);
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.addView(thumbnails, Ui.matchWrap());
        Ui.add(view, scroll, ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(activity, 86), 6);
        photoCount = Ui.text(activity, "0/9", 12, Ui.MUTED, false);
        view.addView(photoCount);
        LinearLayout photoButtons = Ui.horizontal(activity);
        Button camera = Ui.button(activity, "拍照添加", false);
        camera.setOnClickListener(v -> startCamera());
        LinearLayout.LayoutParams cameraParams = Ui.weightParams(Ui.dp(activity, 46));
        cameraParams.setMarginEnd(Ui.dp(activity, 8));
        photoButtons.addView(camera, cameraParams);
        Button gallery = Ui.button(activity, "相册添加", false);
        gallery.setOnClickListener(v -> startGallery());
        photoButtons.addView(gallery, Ui.weightParams(Ui.dp(activity, 46)));
        Ui.add(view, photoButtons, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 8);

        if (product != null) {
            nameInput.setText(product.name);
            purchaseInput.setText(Formatters.editableMoney(product.purchasePriceCents));
            saleInput.setText(Formatters.editableMoney(product.salePriceCents));
            styleInput.setText(product.style);
            selectSeason(product.season);
            selectCategory(product.category);
            photoPaths.addAll(product.photoPaths);
        } else if (draft != null) {
            applyDraft(draft);
        } else if (stockInput != null) {
            stockInput.setText("0");
        }
        refreshPhotos();
    }

    public void applyDraft(ProductDraft draft) {
        nameInput.setText(draft.name);
        purchaseInput.setText(Formatters.editableMoney(draft.purchasePriceCents));
        saleInput.setText(Formatters.editableMoney(draft.salePriceCents));
        if (stockInput != null) stockInput.setText(String.valueOf(draft.stock));
        styleInput.setText(draft.style);
        selectSeason(draft.season);
        selectCategory(draft.category);
        photoPaths.clear();
        photoPaths.addAll(draft.photoPaths);
        refreshPhotos();
    }

    public LinearLayout getView() {
        return view;
    }

    public ProductDraft read() {
        int stock = 0;
        try {
            if (stockInput != null) stock = Integer.parseInt(stockInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("库存必须是整数");
        }
        return new ProductDraft(nameInput.getText().toString(),
                Formatters.parseMoney(purchaseInput.getText().toString()),
                Formatters.parseMoney(saleInput.getText().toString()), stock,
                SEASON_CODES[seasonInput.getSelectedItemPosition()], styleInput.getText().toString(),
                categoryInput.getText().toString(),
                photoPaths.isEmpty() ? "" : photoPaths.get(0), photoPaths);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CAMERA && requestCode != REQUEST_GALLERY) return false;
        if (resultCode == Activity.RESULT_OK) {
            try {
                if (requestCode == REQUEST_CAMERA && cameraUri != null) {
                    addPhoto(PhotoManager.pathFromUri(activity, cameraUri));
                } else if (data != null) {
                    ClipData clip = data.getClipData();
                    if (clip != null) {
                        for (int i = 0; i < clip.getItemCount() && photoPaths.size() < 9; i++) {
                            addPhoto(PhotoManager.copyFromUri(activity, clip.getItemAt(i).getUri()));
                        }
                    } else if (data.getData() != null) {
                        addPhoto(PhotoManager.copyFromUri(activity, data.getData()));
                    }
                }
            } catch (Exception e) {
                Toast.makeText(activity, "图片读取失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        return true;
    }

    private Spinner spinner(String label, String[] values) {
        TextView labelView = Ui.text(activity, label, 14, Ui.MUTED, true);
        Ui.add(view, labelView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);
        Spinner spinner = new Spinner(activity);
        spinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, values));
        Ui.add(view, spinner, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 4);
        return spinner;
    }

    private AutoCompleteTextView autoCompleteField(String label, String[] suggestions, String hint) {
        AutoCompleteTextView input = new AutoCompleteTextView(activity);
        input.setHint(hint);
        input.setTextSize(15);
        input.setTextColor(Ui.TEXT);
        input.setHintTextColor(Ui.MUTED);
        input.setSingleLine(true);
        input.setThreshold(1);
        input.setPadding(Ui.dp(activity, 13), Ui.dp(activity, 9), Ui.dp(activity, 13), Ui.dp(activity, 9));
        input.setBackground(Ui.roundRect(Ui.SURFACE, 11, Ui.BORDER, activity));
        input.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, suggestions));
        TextView labelView = Ui.text(activity, label, 14, Ui.MUTED, true);
        Ui.add(view, labelView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);
        Ui.add(view, input, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 4);
        return input;
    }

    private EditText field(String label, EditText input) {
        TextView labelView = Ui.text(activity, label, 14, Ui.MUTED, true);
        Ui.add(view, labelView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 12);
        Ui.add(view, input, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 4);
        return input;
    }

    private void selectSeason(String code) {
        for (int i = 0; i < SEASON_CODES.length; i++) {
            if (SEASON_CODES[i].equals(code)) {
                seasonInput.setSelection(i);
                return;
            }
        }
    }

    private void selectCategory(String category) {
        categoryInput.setText(category == null ? "" : category, false);
    }

    private void addPhoto(String path) {
        if (photoPaths.size() >= 9) {
            Toast.makeText(activity, "最多添加9张图片", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!photoPaths.contains(path)) photoPaths.add(path);
        refreshPhotos();
    }

    private void refreshPhotos() {
        if (imagePreview == null || thumbnails == null) return;
        Bitmap bitmap = PhotoManager.thumbnail(photoPaths.isEmpty() ? "" : photoPaths.get(0), Ui.dp(activity, 140));
        imagePreview.setImageBitmap(bitmap);
        imagePreview.setContentDescription(bitmap == null ? "未添加商品图片" : "商品封面预览");
        thumbnails.removeAllViews();
        for (int i = 0; i < photoPaths.size(); i++) {
            int index = i;
            LinearLayout item = Ui.vertical(activity);
            ImageView image = new ImageView(activity);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageBitmap(PhotoManager.thumbnail(photoPaths.get(i), Ui.dp(activity, 64)));
            image.setBackground(Ui.roundRect(Ui.SURFACE, 8, i == 0 ? Ui.PRIMARY : Ui.BORDER, activity));
            item.addView(image, new LinearLayout.LayoutParams(Ui.dp(activity, 64), Ui.dp(activity, 58)));
            Button remove = Ui.button(activity, "移除", false);
            remove.setTextSize(10);
            remove.setOnClickListener(v -> {
                photoPaths.remove(index);
                refreshPhotos();
            });
            item.addView(remove, new LinearLayout.LayoutParams(Ui.dp(activity, 64), Ui.dp(activity, 26)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(Ui.dp(activity, 6));
            thumbnails.addView(item, params);
        }
        photoCount.setText(photoPaths.size() + "/9" + (photoPaths.isEmpty() ? "" : " · 第一张为封面"));
    }

    private void startCamera() {
        cameraUri = PhotoManager.createCameraPhoto(activity);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivityForResult(intent, REQUEST_CAMERA);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "没有可用的相机应用", Toast.LENGTH_LONG).show();
        }
    }

    private void startGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            activity.startActivityForResult(intent, REQUEST_GALLERY);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "没有可用的相册应用", Toast.LENGTH_LONG).show();
        }
    }
}
