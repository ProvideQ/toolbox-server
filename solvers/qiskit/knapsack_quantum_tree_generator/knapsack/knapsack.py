import yaml # type: ignore
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict

@dataclass
class Item:
    id: int
    weight: int
    value: int

class KnapsackInstance:
    """
    Items are sorted by value after loading by default.
    """
    def __init__(self, config_path: str | Path | None = None, items: list[Item] | None = None, capacity: int | None = None, sort_by_value: bool = True):
        if config_path:
            with open(config_path, 'r', encoding='utf-8') as f:
                cfg = yaml.safe_load(f)
            if cfg is None:
                raise ValueError(f"Configuration file at {config_path} invalid.")

            self.metadata = cfg.get('metadata', {})
            self.capacity = int(cfg.get('capacity', None))
            self.items = [
                Item(
                    id=None,
                    weight=int(row['weight']),
                    value=int(row['value'])
                )
                for i, row in enumerate(cfg.get('items', []))
            ]
        elif items is not None and capacity is not None:
            self.metadata = {}
            self.capacity = capacity
            self.items = items
        else:
            raise ValueError("Either config_path or (items and capacity) must be provided.")

        if sort_by_value:
            self.sort_items_by_value()
        self.assign_ids_by_rank()
        self.num_items = len(self.items)
        self.ids = [it.id for it in self.items]
        self.weights = [it.weight for it in self.items]
        self.values = [it.value for it in self.items]


    def get_item_by_id(self, item_id: int):
        """Return item with given id, or None if not found."""
        return next((it for it in self.items if it.id == item_id), None)
    
    def get_properties_dict(self) -> Dict[str, Any]:
        """Return instance properties as a dictionary."""
        return {
            'metadata': self.metadata,
            'capacity': self.capacity,
            'items': [it.__dict__ for it in self.items]
        }
        
    def assign_ids_by_rank(self) -> None:    
        """Assign item ids based on current order in self.items if id is None. Used to sort by density"""
        for new_id, it in enumerate(self.items):
            if it.id is None:
                it.id = new_id

    def sort_items_by_density(self) -> None:
        """Sort items in-place by value/weight density in descending order."""
        self.items.sort(key=lambda it: it.value / it.weight if it.weight > 0 else 0, reverse=True)
    
    def sort_items_by_value(self) -> None:
        """Sort items in-place by value in descending order."""
        self.items.sort(key=lambda it: it.value, reverse=True)

    def is_sorted_by_density(self) -> bool:
        """Check if items are sorted by value/weight density in descending order."""
        return all((self.items[i].value / self.items[i].weight if self.items[i].weight > 0 else 0) >=
                   (self.items[i + 1].value / self.items[i + 1].weight if self.items[i + 1].weight > 0 else 0)
                   for i in range(len(self.items) - 1))
        
    def is_sorted_by_value(self) -> bool:
        """Check if items are sorted by value in descending order."""
        return all(self.items[i].value >= self.items[i + 1].value for i in range(len(self.items) - 1))
    
    def _copy(self) -> 'KnapsackInstance':
        """Create a deep copy of the KnapsackInstance."""
        new_instance = KnapsackInstance.__new__(KnapsackInstance)
        new_instance.metadata = self.metadata.copy()
        new_instance.capacity = self.capacity
        new_instance.items = [Item(id=it.id, weight=it.weight, value=it.value) for it in self.items]
        new_instance.num_items = self.num_items
        new_instance.ids = self.ids.copy()
        new_instance.weights = self.weights.copy()
        new_instance.values = self.values.copy()
        return new_instance
