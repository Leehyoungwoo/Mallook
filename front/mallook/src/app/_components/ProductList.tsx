import React from 'react'
import styles from "./productList.module.css";

import {ProductList} from "@/constants";
import ProductComponent from "./Product";
import {Product} from "@/types";

export default function ProductListComponent(props: {productList: Product[]}) {

	const productList = () => {
		if (props.productList.length) {
			console.log(props.productList)
			return (
				props.productList.map((product, index) => {
					return (
						<ProductComponent productData={product} key={index} id={index} />
					);
				})
			)
		// } else {
		// 	return (
		// 		ProductList.map((product, index) => {
		// 			return (
		// 				<ProductComponent productData={product} key={index} id={index} />
		// 			);
		// 		})
		// 	);
		}
	}

	return (
		<div className={styles.productList__container}>
			{productList()}
		</div>
	)
}