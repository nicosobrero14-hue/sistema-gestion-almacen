package com.gestionalmacen.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.gestionalmacen.dto.ProductDTO;
import com.gestionalmacen.entity.Product;
import com.gestionalmacen.entity.Supplier;
import com.gestionalmacen.exception.BusinessRuleException;
import com.gestionalmacen.exception.NotFoundException;
import com.gestionalmacen.repository.IProductRepository;

@Service
public class ProductService implements IProductService {

	@Autowired
	private IProductRepository productRepository;

	// Para buscar el proveedor que se le asigna al producto.
	@Autowired
	private ISupplierService supplierService;

	@Override
	public List<Product> getProducts(String search, boolean activeOnly) {
		return productRepository.search(search.trim(), activeOnly);
	}

	@Override
	public Product findProduct(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("No existe el producto con id " + id));
	}

	@Override
	public Product saveProduct(ProductDTO productDTO, boolean isAdmin) {
		// CU-19: poner un producto en oferta es una decision del administrador.
		if (productDTO.isOnOffer() && !isAdmin) {
			throw new AccessDeniedException("Solo el administrador puede poner un producto en oferta.");
		}
		if (productDTO.getBarcode() != null && productRepository.existsByBarcode(productDTO.getBarcode())) {
			throw new BusinessRuleException("Ya existe un producto con el código de barras " + productDTO.getBarcode());
		}

		Product product = new Product();
		// El stock se carga solo en el alta. Despues se ajusta desde la pantalla de stock.
		product.setStock(productDTO.getStock());
		this.copyData(productDTO, product);
		return productRepository.save(product);
	}

	@Override
	public Product editProduct(Long id, ProductDTO productDTO, boolean isAdmin) {
		Product product = this.findProduct(id);

		// CU-19 y CU-20: solo el administrador cambia el precio o la oferta.
		if (!isAdmin && this.changesPrice(product, productDTO)) {
			throw new AccessDeniedException("Solo el administrador puede modificar el precio y la oferta.");
		}

		if (productDTO.getBarcode() != null && productRepository.existsByBarcodeAndIdNot(productDTO.getBarcode(), id)) {
			throw new BusinessRuleException("Ya existe otro producto con el código de barras " + productDTO.getBarcode());
		}

		this.copyData(productDTO, product);
		return productRepository.save(product);
	}

	@Override
	public void deactivateProduct(Long id) {
		Product product = this.findProduct(id);
		product.setActive(false);
		productRepository.save(product);
	}

	@Override
	public void activateProduct(Long id) {
		Product product = this.findProduct(id);
		product.setActive(true);
		productRepository.save(product);
	}

	// Pasa los datos del DTO a la entidad. Lo usan el alta y la modificacion. El stock no se toca.
	private void copyData(ProductDTO productDTO, Product product) {
		// CU-19: una oferta tiene que tener precio y ser mas barata que el precio normal.
		if (productDTO.isOnOffer() && (productDTO.getOfferPrice() == null
				|| productDTO.getOfferPrice().compareTo(productDTO.getPrice()) >= 0)) {
			throw new BusinessRuleException("El precio de oferta es obligatorio y tiene que ser menor al precio normal.");
		}

		product.setName(productDTO.getName());
		product.setDescription(productDTO.getDescription());
		product.setPrice(productDTO.getPrice());
		product.setMinimumStock(productDTO.getMinimumStock());
		product.setBarcode(productDTO.getBarcode());
		product.setExpirationDate(productDTO.getExpirationDate());
		product.setOnOffer(productDTO.isOnOffer());
		product.setOfferPrice(productDTO.isOnOffer() ? productDTO.getOfferPrice() : null);
		product.setSupplier(this.findActiveSupplier(productDTO.getSupplierId()));
	}

	// true si el DTO trae un precio o una oferta distinta de la que tiene el producto.
	private boolean changesPrice(Product product, ProductDTO productDTO) {
		return !this.sameAmount(product.getPrice(), productDTO.getPrice())
				|| product.isOnOffer() != productDTO.isOnOffer()
				|| !this.sameAmount(product.getOfferPrice(), productDTO.getOfferPrice());
	}

	// Compara importes: 10.0 y 10.00 son el mismo precio. Dos vacios tambien son iguales.
	private boolean sameAmount(BigDecimal first, BigDecimal second) {
		if (first == null || second == null) {
			return first == second;
		}
		return first.compareTo(second) == 0;
	}

	// El proveedor es opcional. Si viene uno, tiene que existir y estar activo.
	private Supplier findActiveSupplier(Long supplierId) {
		if (supplierId == null) {
			return null;
		}

		Supplier supplier = supplierService.findSupplier(supplierId);
		if (!supplier.isActive()) {
			throw new BusinessRuleException("El proveedor " + supplier.getName() + " está dado de baja.");
		}
		return supplier;
	}
}
