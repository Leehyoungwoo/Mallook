import 'dart:async';

import 'package:flutter/material.dart';
import 'package:mallook/constants/gaps.dart';
import 'package:mallook/constants/sizes.dart';
import 'package:mallook/feature/home/api/home_api_service.dart';
import 'package:mallook/feature/home/widgets/my_main_script_widget.dart';
import 'package:mallook/feature/home/widgets/product_widget.dart';
import 'package:mallook/feature/product/model/product.dart';
import 'package:mallook/feature/script/model/script.dart';
import 'package:mallook/global/widget/custom_circular_wait_widget.dart';

class HomeMyScreen extends StatefulWidget {
  const HomeMyScreen({super.key});

  @override
  State<HomeMyScreen> createState() => _HomeMyScreenState();
}

class _HomeMyScreenState extends State<HomeMyScreen> {
  final ScrollController _scrollController = ScrollController();
  final List<Product> _products = [];
  final Future<Script> _script = HomeApiService.getMySingleScript();

  int _productPage = 0;
  int _totalPage = 0;
  bool _isProductLoading = false;

  @override
  void initState() {
    super.initState();

    _scrollController.addListener(() {
      if (_scrollController.offset >=
              (_scrollController.position.maxScrollExtent * 0.9) &&
          !_scrollController.position.outOfRange) {
        _loadMoreProducts();
      }
    });
    _loadMoreProducts();
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _loadMoreProducts() async {
    if (_productPage > _totalPage) return;
    if (!_isProductLoading) {
      if (mounted) {
        setState(() {
          _isProductLoading = true;
        });
      }

      try {
        var loadedProducts =
            await HomeApiService.getPopularProducts(_productPage);
        if (mounted) {
          setState(() {
            _productPage = loadedProducts.currentPage! + 1;
            _totalPage = loadedProducts.totalPage!;
            _products.addAll(
                loadedProducts.content!); // 기존 _products List에 새로운 제품 추가
          });
        }
      } finally {
        _isProductLoading = false;
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: Sizes.size20,
        ),
        child: SingleChildScrollView(
          controller: _scrollController,
          child: Column(
            children: [
              Gaps.v10,
              MyMainScriptWidget(script: _script),
              Gaps.v6,
              Divider(
                height: Sizes.size1,
                color: Colors.grey.shade300,
              ),
              Gaps.v14,
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  crossAxisSpacing: Sizes.size16,
                  mainAxisSpacing: Sizes.size10,
                  childAspectRatio: 0.73,
                ),
                itemBuilder: (context, index) => ProductWidget(
                  product: _products[index],
                ),
                itemCount: _products.length, // itemCount 수정
              ),
              if (_isProductLoading) CustomCircularWaitWidget(),
            ],
          ),
        ),
      ),
    );
  }
}
