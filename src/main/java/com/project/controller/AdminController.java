package com.project.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.project.dto.ProductDt;
import com.project.entity.Admin;
import com.project.entity.Category;
import com.project.entity.Product;
import com.project.service.AdminService;
import com.project.service.CategoryService;
import com.project.service.ProductService;

@Controller
public class AdminController {

    @Autowired
    private CategoryService cservice;

    @Autowired
    private ProductService pservice;

    @Autowired
    private AdminService aservice;

    public static String uploadDir =
            System.getProperty("user.dir") +
            "/src/main/resources/static/productImages";


    // ================= LOGIN =================

    @GetMapping("/admin")
    public String admin() {
        return "login";
    }

    @RequestMapping("/register")
    public String register(String email,String password) {

        if(!(email==null && password==null)) {

            Admin a=new Admin();
            a.setEmail(email);
            a.setPassword(password);
            aservice.save(a);
        }

        return "register";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("email")String email,
                        @RequestParam("password") String password,
                        Model model) {

        List<Admin> list = aservice.fetchAll();

        for(Admin a:list) {

            if(a.getEmail().equals(email)
                    && a.getPassword().equals(password)) {

                model.addAttribute("userobject", a);
                return "admin";
            }
        }

        return "login";
    }


    // ================= CATEGORY =================

    @GetMapping("/admin/categories")
    public String categorypage(Model model) {

        model.addAttribute("categories", cservice.getAll());
        return "categories";
    }

    @GetMapping("/admin/categories/add")
    public String AddCategory(Model model) {

        model.addAttribute("category", new Category());
        return "categoriesAdd";
    }

    @PostMapping("/admin/categories/add")
    public String postAddCategory(@ModelAttribute("category") Category c) {

        cservice.saveCategory(c);
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") int id) {

        cservice.deletebyId(id);
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/categories/update/{id}")
    public String updateCategory(@PathVariable("id") int id, Model model) {

        Optional<Category> category = cservice.fetchbyId(id);

        if(category.isPresent()) {

            model.addAttribute("category", category.get());
            return "categoriesAdd";
        }

        return "error";
    }


    // ================= PRODUCTS =================

    @GetMapping("/admin/products")
    public String productPage(Model model) {

        model.addAttribute("products", pservice.getAll());
        return "products";
    }

    @GetMapping("/admin/products/add")
    public String AddProduct(Model model) {

        model.addAttribute("productDTO", new ProductDt());
        model.addAttribute("categories", cservice.getAll());

        return "productsAdd";
    }


    // ================= ADD / UPDATE PRODUCT =================

    @PostMapping("/admin/products/add")
    public String postAddproduct(
            @ModelAttribute("productDTO") ProductDt p,
            @RequestParam("productImage") MultipartFile file,
            @RequestParam(value="imgName", required=false) String imgName
    ) throws IOException {

        Product pro = new Product();

        pro.setId(p.getId());
        pro.setName(p.getName());
        pro.setPrice(p.getPrice());
        pro.setDescription(p.getDescription());
        pro.setWeight(p.getWeight());
        pro.setCategory(
                cservice.fetchbyId(p.getCategoryId()).get());

        String imageUUID = null;

        // ===== NEW IMAGE UPLOAD =====
        if(file != null && !file.isEmpty()) {

            String originalFileName = file.getOriginalFilename();

            String extension =
                    originalFileName.substring(
                            originalFileName.lastIndexOf("."));

            imageUUID = UUID.randomUUID().toString() + extension;

            Path path = Paths.get(uploadDir, imageUUID);

            Files.write(path, file.getBytes());
        }

        // ===== KEEP OLD IMAGE =====
        else if(imgName != null && !imgName.equals("")) {

            imageUUID = imgName;
        }

        // ===== DEFAULT IMAGE =====
        else {

            imageUUID = "default.png";
        }

        pro.setImageName(imageUUID);

        pservice.saveProduct(pro);

        return "redirect:/admin/products";
    }


    // ================= DELETE PRODUCT =================

    @GetMapping("/admin/product/delete/{id}")
    public String deleteProduct(@PathVariable("id") long id) {

        Product product = pservice.fetchbyId(id).get();

        try {

            Path path = Paths.get(uploadDir, product.getImageName());
            Files.deleteIfExists(path);

        } catch(Exception e) {
            e.printStackTrace();
        }

        pservice.deletebyId(id);

        return "redirect:/admin/products";
    }


    // ================= UPDATE PRODUCT PAGE =================

    @GetMapping("/admin/product/update/{id}")
    public String updateProduct(@PathVariable("id") long id,
                                Model model) {

        Product pro = pservice.fetchbyId(id).get();

        ProductDt pdt = new ProductDt();

        pdt.setId(pro.getId());
        pdt.setName(pro.getName());
        pdt.setPrice(pro.getPrice());
        pdt.setWeight(pro.getWeight());
        pdt.setDescription(pro.getDescription());
        pdt.setCategoryId(pro.getCategory().getId());
        pdt.setImageName(pro.getImageName());

        model.addAttribute("categories", cservice.getAll());
        model.addAttribute("productDTO", pdt);

        return "productsAdd";
    }
}
