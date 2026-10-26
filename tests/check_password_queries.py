"""Execute the Room DAO's real SQL against an isolated SQLite fixture."""
import re
import sqlite3
import unittest
from pathlib import Path

DAO = Path(__file__).resolve().parents[1] / "app/src/main/java/com/example/passcard/data/PasswordDao.kt"
QUERIES = {
    match["method"]: match["multiline"] or match["single"]
    for match in re.finditer(
        r'@Query\((?:"""(?P<multiline>[\s\S]*?)"""|"(?P<single>[^"\n]*)")\)\s*'
        r'(?:suspend\s+)?fun\s+(?P<method>\w+)', DAO.read_text(encoding="utf-8")
    )
}


class PasswordQueriesTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.db.execute("CREATE TABLE passwords (id TEXT, name TEXT, username TEXT, phone TEXT, email TEXT, password TEXT, category TEXT, note TEXT, updatedAt INTEGER)")
        self.db.executemany(
            "INSERT INTO passwords VALUES (?, ?, '', '', '', '', ?, '', ?)",
            [(f"recent-{i}", "Recent", "Other", i + 100) for i in range(40)] + [
                ("legacy-work", "Example account", "工作", 3),
                ("english-work", "Example personal", " Work ", 2),
                ("old-finance", "Example finance", "Finance", 1),
                ("uncategorized", "No category", " ", 0),
                ("custom-all", "Named All", "All", 0),
            ],
        )

    def tearDown(self):
        self.db.close()

    def query(self, method, *, query="", aliases=("",), filter_category=False, field="name", limit=20, matched_categories=None):
        sql = QUERIES[method].replace(":categories", ",".join(f":category{i}" for i in range(len(aliases))))
        matched_categories = matched_categories or (query,)
        sql = sql.replace(":matchedCategories", ",".join(f":matched{i}" for i in range(len(matched_categories))))
        params = dict(query=query, contains=f"%{query}%", prefix=f"{query}%", filterCategory=filter_category, field=field, limit=limit)
        params.update({f"category{i}": value for i, value in enumerate(aliases)})
        params.update({f"matched{i}": value for i, value in enumerate(matched_categories)})
        return [row[0] for row in self.db.execute(sql, params)]

    def test_category_finds_records_outside_recent_preview(self):
        self.assertNotIn("legacy-work", self.query("getRecentPasswords", limit=30))
        self.assertEqual(["legacy-work", "english-work"], self.query("getPasswordsByCategory", aliases=("Work", "工作")))

    def test_search_and_category_are_intersected_in_preview_and_paging(self):
        for method in ("searchPasswords", "getPagedPasswords", "searchPasswordsByField", "getPagedPasswordsByField"):
            with self.subTest(method=method):
                self.assertEqual(["legacy-work", "english-work"], self.query(method, query="Example", aliases=("Work", "工作"), filter_category=True))

    def test_all_and_uncategorized_are_distinct(self):
        self.assertEqual(45, len(self.query("getPagedPasswords")))
        self.assertEqual(["uncategorized"], self.query("getPagedPasswords", filter_category=True))
        self.assertEqual(["custom-all"], self.query("getPagedPasswords", aliases=("All",), filter_category=True))

    def test_empty_category_does_not_fall_back_to_all(self):
        self.assertEqual([], self.query("getPagedPasswords", aliases=("No such category",), filter_category=True))

    def test_preview_limit_does_not_limit_complete_list(self):
        self.assertEqual(20, len(self.query("searchPasswords", query="Recent")))
        self.assertEqual(40, len(self.query("getPagedPasswords", query="Recent")))

    def test_field_search_ignores_other_fields(self):
        self.assertEqual([], self.query("searchPasswordsByField", query="Example", field="email"))

    def test_translated_category_labels_are_searchable(self):
        for method in ("searchPasswords", "getPagedPasswords", "searchPasswordsByField", "getPagedPasswordsByField"):
            with self.subTest(method=method):
                self.assertEqual(["legacy-work", "english-work"], self.query(method, query="工作", field="category", matched_categories=("Work", "工作")))


if __name__ == "__main__":
    unittest.main(verbosity=2)
